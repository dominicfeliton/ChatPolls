#!/usr/bin/env bash

###################################
#          EASY CONFIG            #
###################################
# 1) Change SERVERS_BASE_DIR if you want your servers somewhere else.
# 2) Change CHATPOLLS_BASE_DIR if you cloned ChatPolls repo to a different location.

SERVERS_BASE_DIR="/tmp"
CHATPOLLS_BASE_DIR="/home/dominic/ChatPolls"

###################################
#         AUTO-DERIVED VARS       #
###################################
# Server folders (will look like /Users/<YOU>/Documents/<SERVERNAME>_chp_test_server)
SPIGOT_DIR="${SERVERS_BASE_DIR}/spigot_chp_test_server"
MAGMA_DIR="${SERVERS_BASE_DIR}/magma_chp_test_server"
PAPER1132_DIR="${SERVERS_BASE_DIR}/paper1132_chp_test_server"
PAPER_LATEST_DIR="${SERVERS_BASE_DIR}/chp_test_server"
FOLIA_DIR="${SERVERS_BASE_DIR}/folia_chp_test_server"

# Plugin jar locations (all within ChatPolls repo folders).
CHATPOLLS_SPIGOT_JAR="${CHATPOLLS_BASE_DIR}/spigot-output/ChatPolls-spigot.jar"
CHATPOLLS_PAPER_JAR="${CHATPOLLS_BASE_DIR}/paper-output/ChatPolls-paper.jar"
CHATPOLLS_FOLIA_JAR="${CHATPOLLS_BASE_DIR}/folia-output/ChatPolls-folia.jar"

###################################
#        HELPER FUNCTION          #
###################################
# setup_mc_script:
#   1) Ensures server_dir exists.
#   2) If run.sh is missing, clones the script from GitHub into a temp folder,
#      then copies it in.
#   3) Writes MC version to current_version.txt
#   4) Injects or overwrites "export PROJECT_NAME=..." and "export SERVER_DIR=..."
###################################
#!/usr/bin/env bash

setup_mc_script() {
  local server_dir="$1"      # e.g. /Users/you/Documents/spigot_chp_test_server
  local project_name="$2"    # e.g. "spigot", "paper", "folia"
  local mc_version="$3"      # e.g. "1.13.2"

  # 1) Attempt to create the folder
  if [[ ! -d "$server_dir" ]]; then
    echo "[setup_mc_script] Creating folder: $server_dir"
    mkdir -p "$server_dir"
    if [[ $? -ne 0 ]]; then
      echo "[setup_mc_script] ERROR: Could not create directory '$server_dir'. Permission denied or invalid path."
      return 1
    fi
    echo "[setup_mc_script] Cloning dominicfeliton/minecraft-server-script => $server_dir ..."
    git clone --depth=1 https://github.com/dominicfeliton/minecraft-server-script "$server_dir"
    if [[ $? -ne 0 ]]; then
      echo "[setup_mc_script] ERROR: git clone failed (check permissions, network, or repository existence)."
      return 1
    fi
  fi

  # 2) If we still don't have a valid server_dir, bail
  if [[ ! -d "$server_dir" ]]; then
    echo "[setup_mc_script] ERROR: '$server_dir' does not exist (creation or clone failed?). Aborting."
    return 1
  fi

  # 3) If run.sh not found, do a temp clone and copy
  if [[ ! -f "$server_dir/run.sh" ]]; then
    echo "[setup_mc_script] run.sh not found in $server_dir => clone fresh script to a temp folder..."
    local tempdir
    tempdir="$(mktemp -d)"
    git clone --depth=1 https://github.com/dominicfeliton/minecraft-server-script "$tempdir"
    if [[ $? -ne 0 ]]; then
      echo "[setup_mc_script] ERROR: git clone failed (temp clone)."
      rm -rf "$tempdir"
      return 1
    fi
    # Attempt to copy
    cp -r "$tempdir/"* "$server_dir"/
    if [[ $? -ne 0 ]]; then
      echo "[setup_mc_script] ERROR: Failed copying fresh script files into $server_dir."
      rm -rf "$tempdir"
      return 1
    fi
    rm -rf "$tempdir"
    echo "[setup_mc_script] Copied fresh script files into $server_dir."
  fi

  # 4) If run.sh still doesn’t exist, bail
  if [[ ! -f "$server_dir/run.sh" ]]; then
    echo "[setup_mc_script] ERROR: run.sh not found after copy. Aborting."
    return 1
  fi

  # 5) Overwrite or insert our exports in run.sh (macOS/Linux friendly)
  sed -i.bak '/^export PROJECT_NAME=/d' "$server_dir/run.sh"
  sed -i.bak '/^export SERVER_DIR=/d'    "$server_dir/run.sh"

  # Insert new lines after the shebang (if present) or at the top.
  if grep -q '^#!' "$server_dir/run.sh"; then
    sed -i.bak "2 i\\
export PROJECT_NAME=\"$project_name\"\\
export SERVER_DIR=\"$server_dir\"\\
" "$server_dir/run.sh"
  else
    sed -i.bak "1 i\\
export PROJECT_NAME=\"$project_name\"\\
export SERVER_DIR=\"$server_dir\"\\
" "$server_dir/run.sh"
  fi

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

###################################
# 1) SETUP EACH SERVER FOLDER
###################################
# Example calls: 
# Uncomment or add new ones for the server(s) you want to manage.

# setup_mc_script "$SPIGOT_DIR"       "spigot" "1.21.4"
# setup_mc_script "$PAPER1132_DIR"    "paper"  "1.13.2"
setup_mc_script "$PAPER_LATEST_DIR" "paper"  "1.21.4"
# setup_mc_script "$FOLIA_DIR"        "folia"  "1.21.4"
# setup_mc_script "$MAGMA_DIR"        "magma"  "1.21.4"  # Example if you want Magma, etc.

###################################
# 2) COPY PLUGIN JARS
###################################
# Create plugins folder if missing
mkdir -p "$SPIGOT_DIR/plugins"       2>/dev/null
mkdir -p "$PAPER1132_DIR/plugins"    2>/dev/null
mkdir -p "$PAPER_LATEST_DIR/plugins" 2>/dev/null
mkdir -p "$FOLIA_DIR/plugins"        2>/dev/null
mkdir -p "$MAGMA_DIR/plugins"        2>/dev/null

# Copy respective ChatPolls jars
cp -v "$CHATPOLLS_SPIGOT_JAR" "$SPIGOT_DIR/plugins"       2>/dev/null
cp -v "$CHATPOLLS_SPIGOT_JAR" "$PAPER1132_DIR/plugins"    2>/dev/null
cp -v "$CHATPOLLS_PAPER_JAR"  "$PAPER_LATEST_DIR/plugins" 2>/dev/null
cp -v "$CHATPOLLS_FOLIA_JAR"  "$FOLIA_DIR/plugins"        2>/dev/null
# cp -v <some-other.jar> "$MAGMA_DIR/plugins"             2>/dev/null

###################################
# 3) START THE MAIN SERVER
###################################
echo "[INFO] Starting main server => $PAPER_LATEST_DIR"
cd "$PAPER_LATEST_DIR" || exit 1
./run.sh start --no-tmux

echo "[INFO] Done."
