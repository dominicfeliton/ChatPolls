#!/usr/bin/env bash

###################################
#           CONFIGURATION         #
###################################
# Adjust these paths as you like.

# Server folders
SPIGOT_DIR="/Users/$USER/Documents/spigot_chp_test_server"
MAGMA_DIR="/Users/$USER/Documents/magma_chp_test_server"
PAPER1132_DIR="/Users/$USER/Documents/paper1132_chp_test_server"
PAPER_LATEST_DIR="/Users/$USER/Documents/chp_test_server"
FOLIA_DIR="/Users/$USER/Documents/folia_chp_test_server"

# Plugin jar locations
CHATPOLLS_SPIGOT_JAR="/Users/$USER/Documents/GitHub/ChatPolls/spigot-output/ChatPolls-spigot.jar"
CHATPOLLS_PAPER_JAR="/Users/$USER/Documents/GitHub/ChatPolls/paper-output/ChatPolls-paper.jar"
CHATPOLLS_FOLIA_JAR="/Users/$USER/Documents/GitHub/ChatPolls/folia-output/ChatPolls-folia.jar"

###################################
#        HELPER FUNCTION          #
###################################
# setup_mc_script:
#   1) Ensures server_dir exists.
#   2) If run.sh is missing, clones the script from GitHub into a temp folder,
#      then copies it in.
#   3) Writes MC version to current_version.txt
#   4) Injects or overwrites "export PROJECT_NAME=..."
###################################
setup_mc_script() {
  local server_dir="$1"      # e.g. /Users/you/Documents/magma_chp_test_server
  local project_name="$2"    # e.g. "spigot", "paper", "folia"
  local mc_version="$3"      # e.g. "1.13.2"

  if [[ ! -d "$server_dir" ]]; then
    echo "[setup_mc_script] Creating folder: $server_dir"
    mkdir -p "$server_dir"
    echo "[setup_mc_script] Cloning dominicfeliton/minecraft-server-script => $server_dir ..."
    git clone --depth=1 https://github.com/dominicfeliton/minecraft-server-script "$server_dir"
  fi

  # If run.sh not found, do a temp clone and copy
  if [[ ! -f "$server_dir/run.sh" ]]; then
    echo "[setup_mc_script] run.sh not found in $server_dir => clone fresh script to a temp folder..."
    local tempdir
    tempdir="$(mktemp -d)"
    git clone --depth=1 https://github.com/dominicfeliton/minecraft-server-script "$tempdir"
    if [[ $? -ne 0 ]]; then
      echo "[setup_mc_script] ERROR: git clone failed."
      rm -rf "$tempdir"
      return
    fi
    cp -r "$tempdir/"* "$server_dir"/
    rm -rf "$tempdir"
    echo "[setup_mc_script] Copied fresh script files into $server_dir."
  fi

  # --- Overwrite or insert our exports in run.sh (macOS/Linux friendly) ---

  # 1) Remove any old lines that set these exports (avoid duplicates).
  sed -i.bak '/^export PROJECT_NAME=/d' "$server_dir/run.sh"
  sed -i.bak '/^export SERVER_DIR=/d'    "$server_dir/run.sh"

  # 2) Figure out where to insert:
  #    If there's a shebang as the first line, we insert after line 1.
  #    Otherwise, we insert at line 1.
  if grep -q '^#!' "$server_dir/run.sh"; then
    # Insert after line 1
    sed -i.bak "2 i\\
  export PROJECT_NAME=\"$project_name\"\\
  export SERVER_DIR=\"$server_dir\"\\
  " "$server_dir/run.sh"
  else
    # Insert at top of file
    sed -i.bak "1 i\\
  export PROJECT_NAME=\"$project_name\"\\
  export SERVER_DIR=\"$server_dir\"\\
  " "$server_dir/run.sh"
  fi

  # 3) macOS quirk:
  #    On macOS, `sed -i.bak "2 i\..."` works best with a backslash+newline.
  #    If using GNU sed on Linux, it should also be fine.
  #    Make sure you keep those backslashes + newlines exactly.

  rm -f "$server_dir/run.sh.bak"
}

###################################
#        MAIN SCRIPT LOGIC        #
###################################

# Detect OS for macOS vs. Linux differences
if [[ "$OSTYPE" == "darwin"* ]]; then
  echo "[INFO] Running on macOS..."
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
  echo "[INFO] Running on Linux..."
else
  echo "[ERROR] Unsupported operating system: $OSTYPE"
  exit 1
fi

# 1) Setup each server folder
#setup_mc_script "$SPIGOT_DIR"       "spigot" "1.21.4"
#setup_mc_script "$PAPER1132_DIR"    "paper"  "1.13.2"
setup_mc_script "$PAPER_LATEST_DIR" "paper"  "1.21.4"
#setup_mc_script "$FOLIA_DIR"        "folia"  "1.21.4"

# 2) Copy plugin jars
mkdir -p "$SPIGOT_DIR/plugins"        2>/dev/null
mkdir -p "$PAPER1132_DIR/plugins"     2>/dev/null
mkdir -p "$PAPER_LATEST_DIR/plugins"  2>/dev/null
mkdir -p "$FOLIA_DIR/plugins"         2>/dev/null

cp -v "$CHATPOLLS_SPIGOT_JAR" "$SPIGOT_DIR/plugins"
cp -v "$CHATPOLLS_SPIGOT_JAR" "$PAPER1132_DIR/plugins"
cp -v "$CHATPOLLS_PAPER_JAR"  "$PAPER_LATEST_DIR/plugins"
cp -v "$CHATPOLLS_FOLIA_JAR"  "$FOLIA_DIR/plugins"

# 3) Finally, start ONLY the main server (PAPER_LATEST_DIR)
echo "[INFO] Starting main server => $PAPER_LATEST_DIR"
cd "$PAPER_LATEST_DIR" || exit 1
./run.sh start --no-tmux

echo "[INFO] Done."