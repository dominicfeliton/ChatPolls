# Run maven build
mvn clean package -D"yamltranslator.skip"=true

# Check if build was successful
if [ $? -eq 0 ]; then
    echo "Build successful, copying files..."
    ./dev/copy_chp_macos_linux_wsl.sh paper
else
    echo "Build failed, not copying files"
    exit 1
fi
