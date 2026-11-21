# Changelog

All versions are for Minecraft 1.21.1 (NeoForge). File names: `better_origins-1.21.1-<version>.jar`.

## 0.9.2
- Road Runner: a whoosh when you start sprinting with Speed Burst, then a rushing-wind loop that gets louder the
  faster you run.
- Cave Worm: toxic activation, bubbling loop and drain-away deactivation sounds for Cave Parasites.
- Angel: cold holy sounds for Blessing Self, warm hopeful sounds for Blessing Others, and an energising loop when
  both are on. Loops crossfade when the modes change.
- Cave Worm: Cave Parasites radius 6 blocks (was 5), stronger damage, parasites wear off 3 times slower (1 level
  every 1.5 s); surface sprinting is 15% slower instead of 25% (at -25% sprinting was no faster than walking).
- All these sounds are heard by nearby players (the Self loop only by the Angel) and cost no network traffic.

## 0.9.1
- Ability checks skip mobs, items and arrows without origin data straight away (less work every tick).
- Road Runner: wall crashes are detected reliably (real speed, the whole side of the body, walls at feet+head or
  head+above) and hurt more; the stun no longer zooms the stunned player's view and stunned
  mobs stay perfectly still (their AI is paused and given back when the stun ends).
- Cave Worm: the parasites now deal their own, stronger damage, so they also hurt undead mobs and spiders (immune to Poison);
  infected mobs give off green particles.
- Cooldown bars for Cave Parasites and the Angel's blessings; the Angel's light also shines in selfish mode.
- Performance: the Cave Worm and Angel zone rings are drawn on each player's game instead of being sent by the
  server (hundreds of fewer network packets per second).
- Fixes: no wall crash damage while swimming, flying, riding or after a teleport; creative/spectator players can't
  be stunned; a protected (cancelled) block break no longer leaves Fortune +1 active; the Angel's light comes back
  if another Angel's light was removed nearby; Angel Regeneration heals at the normal rate.

## 0.9.0
- New origin: **Road Runner** (Speed Burst: 3x sprint, runs up full blocks, stunning strikes, wall crashes).
- New origin: **Cave Worm** (stronger underground, weaker under the open sky, Fortune +1, Cave Parasites with a
  10-level infection bar and a cooldown bar).
- New origin: **Angel** (Soft Wings, selfish and altruistic blessings with a blessing menu, glowing golden zone,
  cooldown bar).
- New status effects: Stunned, Angelic Swiftness, Zeal, Might and Feather.

## 0.8.1
- Performance: entities without an origin no longer get empty origin data saved in the world and sent to
  every player; the hover check skips them every tick.
- File names now include the Minecraft version.

## 0.8.0
- New origin: **Superior Bat** (night flight with sprint, Echolocation, glide after flight, night vision,
  burns in the sun).
- Fixed Origins' creative flight power (flight is really stopped when the power turns off).

## 0.7.1
- New ability bars and icons, Elytrian wind charge bar split in 3, Feline leap trail, purple Shulk
  inventory frame, Shulk levitation effect, Perfect Human red screen effect for the whole Adrenaline,
  Phantom colours.
- Ability sounds: Elytrian launch, Arachnid web, Shulk levitation, Feline leap, Blazeborn fireball.
- Performance: ability data sent to clients less often, unsafe shared cache removed in singleplayer,
  particles only sent to nearby players.

## 0.7.0
- New origin menu: origins grouped by impact (Standard, Low, Medium, High) with a coloured background
  for each origin; new Feline icon.

## 0.6.0
- Perfect Human Adrenaline sounds and effects (firework burst, red screen effect), new Perfect Human
  icon.
- Elytrian Wind Charge (3 charges). Blazeborn fireball cooldown 3 seconds. New mod logo.

## 0.5.0
- Jupiter config library replaced by NeoForge's own config; mod information and credits.

## 0.4.x
- Reworked origins, The Perfect Human, Sun Protection and Evolved Gills enchantments, Avian removed, fixes.
