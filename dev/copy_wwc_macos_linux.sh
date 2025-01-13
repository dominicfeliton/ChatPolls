#!/bin/bash

# --------------------------------------------------
# Helper function: clone & configure run.sh if needed
# --------------------------------------------------
setup_mc_script() {
  local server_dir="$1"       # e.g. /Users/$USER/Documents/spigot_wwc_test_server
  local project_name="$2"     # e.g. "spigot", "paper", "folia"
  local mc_version="$3"       # e.g. "1.13.2"

  # If the directory doesn't exist at all, create it and do a normal clone
  if [[ ! -d "$server_dir" ]]; then
    echo "[setup_mc_script] Creating directory: $server_dir"
    mkdir -p "$server_dir"
    echo "[setup_mc_script] Cloning dominicfeliton/minecraft-server-script into $server_dir ..."
    git clone --depth=1 https://github.com/dominicfeliton/minecraft-server-script "$server_dir"
  fi

  # If run.sh is still missing, then we do a temp clone and copy over
  if [[ ! -f "$server_dir/run.sh" ]]; then
    echo "[setup_mc_script] run.sh not found in $server_dir => cloning fresh script to a temp folder..."
    local tempdir
    tempdir="$(mktemp -d)"
    git clone --depth=1 https://github.com/dominicfeliton/minecraft-server-script "$tempdir"
    if [[ $? -ne 0 ]]; then
      echo "[setup_mc_script] ERROR: git clone to tempdir failed."
      rm -rf "$tempdir"
      return
    fi
    # Copy only the relevant files (run.sh, LICENSE, README.md).
    # Or copy everything if you prefer:
    cp -r "$tempdir/." "$server_dir/"
    rm -rf "$tempdir"
    echo "[setup_mc_script] Copied fresh script files into $server_dir."
  fi

  # If we want to pin a specific MC version for this server:
  if [[ -n "$mc_version" ]]; then
    echo "$mc_version" > "$server_dir/current_version.txt"
  fi

  # If run.sh is now present, inject (or update) export PROJECT_NAME=...
  if [[ -f "$server_dir/run.sh" ]]; then
    if ! grep -q '^export PROJECT_NAME=' "$server_dir/run.sh"; then
      # Insert at top
      sed -i.bak "1s;^;export PROJECT_NAME=\"$project_name\"\n;" "$server_dir/run.sh"
    else
      # Or forcibly replace
      sed -i.bak "s|^export PROJECT_NAME=.*|export PROJECT_NAME=\"$project_name\"|" "$server_dir/run.sh"
    fi
    rm -f "$server_dir/run.sh.bak" 2>/dev/null
  else
    echo "[setup_mc_script] WARNING: run.sh is still missing in $server_dir after clone. Check logs above."
  fi
}

# --------------------------------------------------
# Main Script
# --------------------------------------------------

if [[ "$OSTYPE" == "darwin"* ]]; then
    # -------------------------
    # macOS
    # -------------------------
    SPIGOT_DIR="/Users/$USER/Documents/spigot_wwc_test_server"
    #MAGMA_DIR="/Users/$USER/Documents/magma_wwc_test_server"
    PAPER1132_DIR="/Users/$USER/Documents/paper1132_wwc_test_server"
    PAPER_LATEST_DIR="/Users/$USER/Documents/wwc_test_server"
    FOLIA_DIR="/Users/$USER/Documents/folia_wwc_test_server"

    # 1) Setup each directory to have the run.sh script
    #    Adjust MC versions as you prefer:
    setup_mc_script "$SPIGOT_DIR"       "spigot"  "1.21.4"
    #setup_mc_script "$MAGMA_DIR"        "spigot"  "1.12.2"
    setup_mc_script "$PAPER1132_DIR"    "paper"   "1.13.2"
    setup_mc_script "$PAPER_LATEST_DIR" "paper"   "1.21.4"
    setup_mc_script "$FOLIA_DIR"        "folia"   "1.21.4"

    # 2) Copy your ChatPolls jars into plugins
    mkdir -p "$SPIGOT_DIR/plugins"
    #mkdir -p "$MAGMA_DIR/plugins"
    mkdir -p "$PAPER1132_DIR/plugins"
    mkdir -p "$PAPER_LATEST_DIR/plugins"
    mkdir -p "$FOLIA_DIR/plugins"

    cp "/Users/$USER/Documents/GitHub/ChatPolls/spigot-output/ChatPolls-spigot.jar" "$SPIGOT_DIR/plugins"
    #cp "/Users/$USER/Documents/GitHub/ChatPolls/spigot-output/ChatPolls-spigot.jar" "$MAGMA_DIR/plugins"
    cp "/Users/$USER/Documents/GitHub/ChatPolls/spigot-output/ChatPolls-spigot.jar" "$PAPER1132_DIR/plugins"
    cp "/Users/$USER/Documents/GitHub/ChatPolls/paper-output/ChatPolls-paper.jar"   "$PAPER_LATEST_DIR/plugins"
    cp "/Users/$USER/Documents/GitHub/ChatPolls/folia-output/ChatPolls-folia.jar"   "$FOLIA_DIR/plugins"

    # 3) Change directory into wwc_test_server and start
    cd "$PAPER_LATEST_DIR" || exit 1
    ./run.sh start --no-tmux

elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    # -------------------------
    # Linux
    # -------------------------
    PAPER_LATEST_DIR="/home/$USER/Documents/wwc_test_server"
    PAPER1165_DIR="/home/$USER/Documents/wwc_test_server_1165"
    PAPER1132_DIR="/home/$USER/Documents/wwc_test_server_1132"
    SPIGOT_DIR="/home/$USER/Documents/wwc_test_server_spigot"

    setup_mc_script "$PAPER_LATEST_DIR" "paper"  "1.21.4"
    setup_mc_script "$PAPER1165_DIR"    "paper"  "1.16.5"
    setup_mc_script "$PAPER1132_DIR"    "paper"  "1.13.2"
    setup_mc_script "$SPIGOT_DIR"       "spigot" "1.21.4"

    mkdir -p "$PAPER_LATEST_DIR/plugins"
    mkdir -p "$PAPER1165_DIR/plugins"
    mkdir -p "$PAPER1132_DIR/plugins"
    mkdir -p "$SPIGOT_DIR/plugins"

    cp "/home/$USER/Documents/ChatPolls/paper-output/ChatPolls-paper.jar"   "$PAPER_LATEST_DIR/plugins"
    cp "/home/$USER/Documents/ChatPolls/paper-output/ChatPolls-paper.jar"   "$PAPER1165_DIR/plugins"
    cp "/home/$USER/Documents/ChatPolls/spigot-output/ChatPolls-spigot.jar" "$PAPER1132_DIR/plugins"
    cp "/home/$USER/Documents/ChatPolls/spigot-output/ChatPolls-spigot.jar" "$SPIGOT_DIR/plugins"

    cd "$PAPER_LATEST_DIR" || exit 1
    ./run.sh start --no-tmux
else
    echo "Unsupported operating system: $OSTYPE"
    exit 1
fi