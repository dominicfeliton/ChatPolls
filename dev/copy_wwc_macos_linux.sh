#!/bin/bash

# Determine the operating system
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS commands
    cp /Users/$USER/Documents/GitHub/ChatPolls/spigot-output/ChatPolls-spigot.jar /Users/$USER/Documents/spigot_wwc_test_server/plugins
    cp /Users/$USER/Documents/GitHub/ChatPolls/spigot-output/ChatPolls-spigot.jar /Users/$USER/Documents/magma_wwc_test_server/plugins
    cp /Users/$USER/Documents/GitHub/ChatPolls/spigot-output/ChatPolls-spigot.jar /Users/$USER/Documents/paper1132_wwc_test_server/plugins
    cp /Users/$USER/Documents/GitHub/ChatPolls/paper-output/ChatPolls-paper.jar /Users/$USER/Documents/wwc_test_server/plugins
    cp /Users/$USER/Documents/GitHub/ChatPolls/folia-output/ChatPolls-folia.jar /Users/$USER/Documents/folia_wwc_test_server/plugins
    cd /Users/$USER/Documents/wwc_test_server/
    ./run.sh start --no-tmux
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    # Linux commands
    cp /home/$USER/Documents/ChatPolls/paper-output/ChatPolls-paper.jar /home/$USER/Documents/wwc_test_server/plugins
    cp /home/$USER/Documents/ChatPolls/paper-output/ChatPolls-paper.jar /home/$USER/Documents/wwc_test_server_1165/plugins
    cp /home/$USER/Documents/ChatPolls/spigot-output/ChatPolls-spigot.jar /home/$USER/Documents/wwc_test_server_1132/plugins
    cp /home/$USER/Documents/ChatPolls/spigot-output/ChatPolls-spigot.jar /home/$USER/Documents/wwc_test_server_spigot/plugins
    cd /home/$USER/Documents/wwc_test_server/
    ./run.sh start --no-tmux
else
    echo "Unsupported operating system."
fi
