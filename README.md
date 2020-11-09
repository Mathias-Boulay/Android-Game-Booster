<p align="center">
<a href="https://imgbb.com/"><img src="https://i.ibb.co/L1pNz83/roundsplashscreenicon-1-1-1-1-1-1.png" alt="roundsplashscreenicon-1-1-1-1-1-1" border="0"></a></p>


# Game Resolution Scaler (GRS)
The Game Resolution scaler is a unified implementation for custom resolutions on Android, compatible from android 5.0 (*lollipop*) all the way up to android 11, compatible with both rooted **and** rootless devices !

## Features
- FPS gains up to **+80%** !
- Save battery life !
- Lightweight, with an install size < 5MB
- A simple an beautiful UI, easy to understand by everyone
- Automated DPI scaling, avoiding too big/small UI everywhere
- Toggle for stock DPI value &sup1;
- Aggressive Low Memory Killer (LMK) to prioritize RAM for GAMING &sup1; | &sup2;
- Kill every non-system app before launching the game [NOT ADVISED] &sup1; | &sup2;

&sup1; *Optional feature, disabled by default*.

&sup2; *Feature available for rooted devices only*.



## Setup
### Rooted devices

<details> <summary>Click for instruction on rooted devices</summary>

- [Download](https://github.com/serpentspirale/Android-Game-Booster/releases) and install the latest stable build.
- Upon launch, when the app ask for root rights, grant them.
- Profit !

</details>


### Non-rooted devices

<details> <summary>Click here for instructions on non-rooted devices</summary>

**You need to have developer options and USB debugging enabled on your device !**

- [Download](https://github.com/serpentspirale/Android-Game-Booster/releases) and install the latest stable build.
- **Before launching the app:**
	- [Download](https://developer.android.com/studio/releases/platform-tools) and extract the platform-tools adapted to your platform
	- Copy [this file](https://raw.githubusercontent.com/serpentspirale/Android-Game-Booster/main/NON-ROOT-PERMISSION-FILE) and save it as a *.bat* (windows) or a *.sh* (Linux, MacOS) **in the extracted folder !**
	- Link your phone to your PC with the USB cable.
	- Execute the saved file. If the phone asks *allow USB debugging ?* , grant permissions to it. 
	- If all went good, the app should have been granted the *WRITE_SECURE_SETTINGS* permission.
- Launch the app and select non-root when prompted to.
- Profit !

</details>


## How to contribute

If you'd like to contribute, start by searching through the [issues](https://github.com/serpentspirale/Android-Game-Booster/issues) and [pull requests](https://github.com/serpentspirale/Android-Game-Booster/pulls) to see whether someone else has raised a similar idea or question.

If you don't see your idea listed, and you think it fits into the goals of this application, do one of the following:
* **If your contribution is minor,** such as a typo fix, open a pull request.
* **If your contribution is major,** such as a new guide, start by opening an issue first. That way, other people can weigh in on the discussion before you do any work.

Development is made on Android Studio 4.1 with Gradle 6.1.1.
You can just clone the repo and get started !

**Key rules when contributing:**
-  Respect the same style as the original code (better consistency)
- It must introduce no crashes whatsoever (be error proof)
- When introducing UI changes, make sure it is ~~dumb~~ user proof. It seems simple but believe me, it isn't.
- Do **not** touch the *build.gradle* file




### Todos

 - Improve the UI smoothness by caching.
 - Improve performance with multi-threading the getGameApps and showAddPopup functions.
 - Add per-game custom resolution to override global custom resolution.
 - Add compatibility to android 4.4 ? Are modern games compatible with a 7 years old version ?


License
----

See the LICENSE file at the root of the repository
