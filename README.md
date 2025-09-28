Links

About

Versions

Download

FAQ

Other useful files

Rylan’s AltoClef Fork

Plays block game… but smarter.
Powered by Baritone + new features.

A client-side bot that can beat Minecraft on its own — now with extra focus on PvE combat, PvP mechanics, advanced parkour, smoother movement, and more features.

⚠️ This fork is still under active development. Expect bugs and half-finished systems. If you have suggestions, ideas, or find bugs, open an issue or contact me directly.

About this fork

This fork builds on MiranCZ’s Altoclef fork
 and the original Altoclef project
.

My goals:

Stronger PvE combat AI (smarter mob fighting).

Experimental PvP logic (axes vs shields, strafing, timing).

Advanced parkour: sprint chains, head-hitters, ladder/vine grabs, clutch block placement mid-jump.

Optional less robotic movement (yaw/pitch smoothing, timing jitter).

High-level “do whatever it takes” mode for big goals (get diamonds, finish parkour course, grab Elytra, collect armor trims).

Long-term: expanded item goals (armor trims, Elytra runs, more).

Versions

Like the upstream fork, this uses the ReplayMod preprocessor
 to support many Fabric versions.
Target range: 1.16.5 → 1.21.1 (inclusive).

How it works

Altoclef + Baritone together manage goals and pathing.

My fork layers new tasks (combat, trims, Elytra) and parkour injection on top of Baritone.

See the original guide
 for base architecture.

Download

⚠️ Important: remove old Baritone configs before installing. They conflict with Altoclef.

Releases

(work in progress — once I publish my own builds, they’ll be here)

For now, check Releases
.

FAQ
Crashing?

Make sure you downloaded the correct JAR for your Minecraft version.

Don’t add separate Baritone jars — Altoclef bundles it.

Altoclef doesn’t play well with many other mods. Try running it alone.

If you’re still stuck, open an issue and include:

Minecraft version

Altoclef fork version

Crash log or reproduction steps

Why add PvP/PvE back?

This fork’s focus is fun + experimentation. Combat is a core part of Minecraft, so I want the bot to handle it.

Can you add X version?

If Baritone isn’t ported yet, then I can’t. I aim to support everything from 1.16.5+.

Other useful files

Usage Guide

TODO / Future Features

Development Guide
