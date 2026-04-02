const { execSync } = require("node:child_process");
const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");

const projectRoot = path.resolve(__dirname, "..");

function run(command, options = {}) {
  execSync(command, {
    stdio: options.silent ? "ignore" : "inherit",
    env: options.env || process.env,
    cwd: options.cwd || projectRoot,
  });
}

function findAndroidSdkPath() {
  const candidates = [
    process.env.ANDROID_HOME,
    process.env.ANDROID_SDK_ROOT,
    process.platform === "win32"
      ? path.join(process.env.LOCALAPPDATA || "", "Android", "Sdk")
      : path.join(process.env.HOME || "", "Android", "Sdk"),
  ].filter(Boolean);

  return candidates.find((candidate) => fs.existsSync(candidate));
}

function ensureLocalProperties(workspaceRoot) {
  const sdkPath = findAndroidSdkPath();

  if (!sdkPath) {
    throw new Error(
      "Android SDK not found. Install Android Studio SDK or set ANDROID_HOME / ANDROID_SDK_ROOT.",
    );
  }

  const localPropertiesPath = path.join(workspaceRoot, "android", "local.properties");
  const escapedSdkPath = sdkPath.replace(/\\/g, "\\\\");
  fs.writeFileSync(localPropertiesPath, `sdk.dir=${escapedSdkPath}\n`, "utf8");
}

function setupShortWorkspace(workspaceRoot) {
  if (process.platform !== "win32" || workspaceRoot.length < 80) {
    return {
      workspaceRoot,
      cleanup: () => {},
    };
  }

  const driveLetter = "R:";
  const parentRoot = path.dirname(workspaceRoot);
  const mappedWorkspace = path.join(`${driveLetter}\\`, path.basename(workspaceRoot));

  try {
    run(`subst ${driveLetter} /d`, { silent: true });
  } catch {
    // Ignore if not mapped.
  }

  run(`subst ${driveLetter} "${parentRoot}"`);

  return {
    workspaceRoot: mappedWorkspace,
    cleanup: () => {
      try {
        run(`subst ${driveLetter} /d`);
      } catch {
        // Best effort cleanup.
      }
    },
  };
}

const expoEnv = {
  ...process.env,
  CI: process.env.CI || "1",
};

const shortWorkspace = setupShortWorkspace(projectRoot);

try {
  run("npx expo prebuild --platform android", {
    env: expoEnv,
    cwd: shortWorkspace.workspaceRoot,
  });

  ensureLocalProperties(shortWorkspace.workspaceRoot);

  if (os.platform() === "win32") {
    run("gradlew.bat assembleDebug assembleAndroidTest -DtestBuildType=debug", {
      cwd: path.join(shortWorkspace.workspaceRoot, "android"),
    });
  } else {
    run("./gradlew assembleDebug assembleAndroidTest -DtestBuildType=debug", {
      cwd: path.join(shortWorkspace.workspaceRoot, "android"),
    });
  }
} finally {
  shortWorkspace.cleanup();
}
