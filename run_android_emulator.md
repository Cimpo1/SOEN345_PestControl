# Running the App on an Android Emulator

Guide on how to create and run an Android emulator through Android Studio.

## Prerequisites
Before following this guide, make sure:
- Android Studio is installed
- Android SDK and AVD components are installed (via Android Studio)


## Steps to Run on an Android Emulator
### 1. Open Android Studio Device Manager
Launch **Android Studio** and go to either **More Actions → Device Manager**, or **Tools → Device Manager** (depending on version)


### 2. Create a Virtual Device
Click **Create Device** or **Create Virtual Device** 

Choose a phone (recommended to use any modern pixel)


### 3. Choose a System Image
Select any system image with:
  - **x86_64** (recommended for Intel and AMD Windows PCs)
  - **arm64** (recommended for Apple Silicon Macs)

> Use images labeled with Google APIs for best use

- If the image is not downloaded, click **Download** -> **Finish**

### 4. Boot the Emulator
You can start the emulator in either of the following ways:
- Click the start button next to the device in Device Manager
- Start it from VS Code using the device selector
- Using the terminal:
```bash
cd %LOCALAPPDATA%\Android\Sdk\emulator
emulator -avd <emulator name>
```


## Notes

- Only one emulator needs to be running at a time.
- Emulator setup is a one-time process; once created, it can be reused for all future runs and other projects.
