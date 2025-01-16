#!/usr/bin/env bash

###################################
#          EASY CONFIG            #
###################################
# Where to create server folders (e.g., /tmp/spigot_chp_test_server)
SERVERS_BASE_DIR="/tmp"

# Where your ChatPolls repo is
CHATPOLLS_BASE_DIR="/home/dominic/Documents/ChatPolls"

# Default MC version if user doesn't specify one
LATEST_VERSION="1.21.4"

# If you want to redownload the latest version every time
NO_UPDATE=true

###################################
#       CHATPOLLS JAR PATHS       #
###################################
CHATPOLLS_SPIGOT_JAR="${CHATPOLLS_BASE_DIR}/spigot-output/ChatPolls-spigot.jar"
CHATPOLLS_PAPER_JAR="${CHATPOLLS_BASE_DIR}/paper-output/ChatPolls-paper.jar"
CHATPOLLS_FOLIA_JAR="${CHATPOLLS_BASE_DIR}/folia-output/ChatPolls-folia.jar"
# If you have a special JAR for Magma or velocity, define them here:
# CHATPOLLS_VELOCITY_JAR=...

###################################
#         HELPER FUNCTIONS        #
###################################

# usage: prints help info and exits
usage() {
  cat <<EOF
Usage:
  $(basename "$0") <serverChoice> [version]

Available <serverChoice>:
  spigot      => sets up only Spigot
  paper       => sets up only Paper
  folia       => sets up only Folia
  all         => sets up all known server types with default versions
                 (ignores user-supplied version if provided)

If [version] is omitted, uses LATEST_VERSION=$LATEST_VERSION.

Examples:
  # Use default version for Spigot
  $0 spigot
  # Use an explicit version for Paper
  $0 paper 1.21.3
  # Setup and start all known server types with default versions
  $0 all

EOF
  exit 1
}

# setup_mc_script:
#  - Clones or updates dominicfeliton/minecraft-server-script into server_dir
#  - Overwrites run.sh with the correct PROJECT_NAME, SERVER_DIR
#  - If Folia, also sets FOLIA_SRC_DIR and FOLIA_DOCKER_CTX => subfolders
setup_mc_script() {
  local server_dir="$1"    
  local project_name="$2"  
  local mc_version="$3"    

  # 1) Create the server_dir if missing
  if [[ ! -d "$server_dir" ]]; then
    echo "[setup_mc_script] Creating folder: $server_dir" >&2
    mkdir -p "$server_dir"
    if [[ $? -ne 0 ]]; then
      echo "[setup_mc_script] ERROR: Could not create directory '$server_dir'." >&2
      return 1
    fi
    echo "[setup_mc_script] Cloning dominicfeliton/minecraft-server-script => $server_dir ..." >&2
    git clone --depth=1 https://github.com/dominicfeliton/minecraft-server-script "$server_dir"
    if [[ $? -ne 0 ]]; then
      echo "[setup_mc_script] ERROR: git clone failed." >&2
      return 1
    fi
  fi

  # 2) If run.sh not found, do a fresh clone to a temp folder, then copy
  if [[ ! -f "$server_dir/run.sh" ]]; then
    echo "[setup_mc_script] run.sh not found in $server_dir => clone fresh script to temp folder..." >&2
    local tempdir
    tempdir="$(mktemp -d)"
    git clone --depth=1 https://github.com/dominicfeliton/minecraft-server-script "$tempdir"
    if [[ $? -ne 0 ]]; then
      echo "[setup_mc_script] ERROR: temp git clone failed." >&2
      rm -rf "$tempdir"
      return 1
    fi

    cp -r "$tempdir/"* "$server_dir/" || {
      echo "[setup_mc_script] ERROR: copying script files failed." >&2
      rm -rf "$tempdir"
      return 1
    }
    rm -rf "$tempdir"
    echo "[setup_mc_script] Copied fresh script files into $server_dir." >&2
  fi

  # 3) run.sh must exist now
  if [[ ! -f "$server_dir/run.sh" ]]; then
    echo "[setup_mc_script] ERROR: run.sh not found after clone. Aborting." >&2
    return 1
  fi

  # 4) Remove old lines for PROJECT_NAME, SERVER_DIR, FOLIA_SRC_DIR, FOLIA_DOCKER_CTX
  sed -i.bak '/^export PROJECT_NAME=/d'      "$server_dir/run.sh"
  sed -i.bak '/^export SERVER_DIR=/d'        "$server_dir/run.sh"
  sed -i.bak '/^export FOLIA_SRC_DIR=/d'     "$server_dir/run.sh"
  sed -i.bak '/^export FOLIA_DOCKER_CTX=/d'  "$server_dir/run.sh"

  # 5) Insert new env lines
  #    Insert after shebang, or at top if none
  if grep -q '^#!' "$server_dir/run.sh"; then
    sed -i.bak "2 i\\
export PROJECT_NAME=\"$project_name\"\\
export SERVER_DIR=\"$server_dir\"\\
" "$server_dir/run.sh"

    # If it's Folia, also define local FOLIA_SRC_DIR & FOLIA_DOCKER_CTX
    if [[ "$project_name" == "folia" ]]; then
      sed -i.bak "4 i\\
export FOLIA_SRC_DIR=\"\$SERVER_DIR/FoliaSource\"\\
export FOLIA_DOCKER_CTX=\"\$SERVER_DIR/folia_docker_build\"\\
" "$server_dir/run.sh"
    fi
  else
    sed -i.bak "1 i\\
export PROJECT_NAME=\"$project_name\"\\
export SERVER_DIR=\"$server_dir\"\\
" "$server_dir/run.sh"
    if [[ "$project_name" == "folia" ]]; then
      sed -i.bak "3 i\\
export FOLIA_SRC_DIR=\"\$SERVER_DIR/FoliaSource\"\\
export FOLIA_DOCKER_CTX=\"\$SERVER_DIR/folia_docker_build\"\\
" "$server_dir/run.sh"
    fi
  fi

  rm -f "$server_dir/run.sh.bak"
  return 0
}

# setup_and_copy:
#  - Figures out projectName (spigot, paper, folia, magma, etc.)
#  - Creates a server dir name => e.g. /tmp/spigot_chp_test_server
#  - Calls setup_mc_script => clones run.sh if needed
#  - Copies the plugin jar
setup_and_copy() {
  local serverChoice="$1"   # spigot, paper, folia, etc.
  local mcVersion="$2"

  # 1) Determine projectName & pluginJar
  local projectName=""
  local pluginJar=""
  case "$serverChoice" in
    spigot)
      projectName="spigot"
      pluginJar="$CHATPOLLS_SPIGOT_JAR"
      ;;
    paper)
      projectName="paper"
      pluginJar="$CHATPOLLS_PAPER_JAR"
      ;;
    folia)
      projectName="folia"
      pluginJar="$CHATPOLLS_FOLIA_JAR"
      ;;
    *)
      echo "[ERROR] Unknown server type: $serverChoice" >&2
      return 1
      ;;
  esac

  # 2) Build serverDir
  local serverDir="${SERVERS_BASE_DIR}/${serverChoice}_chp_test_server"
  
  # 3) Log to stderr so it won't interfere with our return value
  echo "[INFO] Setting up $serverChoice => $serverDir (version=$mcVersion)" >&2
  
  # 4) Actually set it up
  setup_mc_script "$serverDir" "$projectName" "$mcVersion" || {
    echo "[ERROR] setup_mc_script failed for $serverChoice" >&2
    return 1
  }

  # 5) plugins folder
  mkdir -p "$serverDir/plugins"
  if [[ -f "$pluginJar" ]]; then
    echo "Copying $pluginJar => $serverDir/plugins" >&2
    cp -v "$pluginJar" "$serverDir/plugins/" >&2
  else
    echo "[WARN] Plugin jar not found at $pluginJar => skipping copy." >&2
  fi

  # 6) THE KEY: echo the serverDir to STDOUT as the last line
  echo "$serverDir"
  return 0
}


###################################
#             MAIN                #
###################################
serverChoice="$1"
userVersion="$2"
[[ -z "$serverChoice" ]] && usage

if [[ -z "$userVersion" ]]; then
  userVersion="$LATEST_VERSION"
fi

if [[ "$serverChoice" == "all" ]]; then
  # example for "all"
  declare -a allTypes=("spigot" "paper" "folia")
  # We'll store the final server dir
  MAIN_SERVER_DIR=""
  for stype in "${allTypes[@]}"; do
    # Force usage of LATEST_VERSION, or keep userVersion if you prefer
    localDir="$(setup_and_copy "$stype" "$LATEST_VERSION")"
    if [[ $? -ne 0 ]]; then
      echo "[ERROR] setup_and_copy failed for $stype" >&2
      exit 1
    fi
    MAIN_SERVER_DIR="$localDir"
  done
else
  # Single server
  MAIN_SERVER_DIR="$(setup_and_copy "$serverChoice" "$userVersion")"
  if [[ $? -ne 0 ]]; then
    echo "[ERROR] setup_and_copy failed for $serverChoice" >&2
    exit 1
  fi
fi

# Now MAIN_SERVER_DIR should be a single line with e.g. /tmp/spigot_chp_test_server
echo "[INFO] Starting server => $MAIN_SERVER_DIR" >&2
cd "$MAIN_SERVER_DIR" || exit 1

if [[ "$NO_UPDATE" == "true" ]]; then
  EXTRA_ARGS="--no-update"
else
  EXTRA_ARGS=""
fi

./run.sh start "$userVersion" $EXTRA_ARGS --no-tmux
echo "[INFO] Done." >&2
