# Links
- [About](#about-this-fork)
- [Versions](#versions)
- [Download](#download)
- [FAQ](#faq)
- [Other useful files](#other-useful-files)

# AltoClef Advanced
*Plays block game.*  
*Powered by Baritone.*  

A client-side bot that tries to beat Minecraft on its own... but this fork is focused on **advanced features** like:
- Better PvE and PvP combat
- Smarter & smoother parkour (including block placement mid-jump)
- Elytra acquisition
- Armor trims and future Minecraft features
- General QoL and smarter pathing

**This fork is still under heavy development.** If you have questions, ideas, or bugs, feel free to reach out!  
- Use the [issues](https://github.com/YourName/altoclef-advanced/issues)  
- Or contact me on Discord  

The original AltoClef became [the first bot to beat Minecraft fully autonomously](https://youtu.be/baAa6s8tahA) on May 24, 2021.  
This fork builds on that legacy with new combat, movement, and feature expansions.

**Join the [Discord Server](https://discord.gg/JdFP4Kqdqc)** for discussions, updates, and goofs.

---

## About this fork
This fork aims to **extend AltoClef** with new mechanics and smarter behavior:
- Advanced parkour with human-like smoothing
- PvE/PvP combat improvements
- Elytra and End-game progression tasks
- Armor trims & duplication logic
- “Whatever-it-takes” goal system

Based on [MiranCZ’s fork](https://github.com/MiranCZ/altoclef), which itself optimized [Marvion’s fork](https://github.com/MarvionKirito/altoclef).

---

## The preprocessor
This project uses the [Replay Mod preprocessor](https://github.com/ReplayMod/preprocessor) to stay compatible across multiple Minecraft versions.

### Versions
Available on **Fabric** for versions between `1.21.1` and `1.16.5`.  
If a version in this range is missing, feel free to open an issue.

> [!NOTE]  
> All versions use the same release of AltoClef Advanced, though some rely on older versions of Baritone.  
> See the Baritone fork [here](https://github.com/MiranCZ/baritone_altoclef).

---

## How it works
- [Guide from the wiki](https://github.com/MiranCZ/altoclef/wiki/1:-Documentation:-Big-Picture)  
- [Video explanation](https://youtu.be/q5OmcinQ2ck?t=387)  

---

## Download
> [!IMPORTANT]  
> After installing, move/delete your old Baritone configs if you have them.  
> Preexisting configs will break AltoClef Advanced. This will be fixed in the future.

Latest release builds can be found under [Releases](https://github.com/YourName/altoclef-advanced/releases).  

| Version | Fabric Download Link |
|---------|----------------------|
| 1.21.1  | [Download](#) |
| 1.21    | [Download](#) |
| 1.20.6  | [Download](#) |
| 1.20.5  | [Download](#) |
| 1.20.4  | [Download](#) |
| 1.20.2  | [Download](#) |
| 1.20.1  | [Download](#) |
| 1.19.4  | [Download](#) |
| 1.18.2  | [Download](#) |
| 1.18    | [Download](#) |
| 1.17.1  | [Download](#) |
| 1.16.5  | [Download](#) |

---

## FAQ

### My AltoClef is crashing! What do I do?
- Make sure you downloaded the correct JAR for your Minecraft version.  
- You **do not** need to add Baritone separately; it’s included.  
- If you’re mixing this with other mods, crashes may happen.  
- If the issue persists, open an [issue](https://github.com/YourName/altoclef-advanced/issues) with:
  - Your Minecraft + Altoclef Advanced version  
  - What happened  
  - Crash log  

### Will PvP / player attacking be supported?
Yes — unlike the upstream fork, this version will include optional PvP logic for private servers with consent.

### Can you add version X of Minecraft?
We support 1.16.5 → latest.  
For brand-new versions, Baritone must be ported first before AltoClef Advanced can support it.

---

## Other useful files
- [Usage Guide](usage.md)  
- [TODO / Future Features](TODO.md)  
- [Development Guide](develop.md)  
