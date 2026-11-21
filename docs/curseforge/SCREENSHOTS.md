# Tricky Trials Backport: screenshot scenes

Hand-built scenes for every feature the tester's photos don't cover (`PHOTOS.md`, "No usable shot"). The tester's
photos keep their slots in `DESCRIPTION.md`; these fill the sections that have none.

**World:** `Tricky Trials Showcase` in the Backport Showcase instance (OptiFine), seed 2111, Caves & Cliffs world type.
Built on `G:\VBPTest\showcase121` (Core 1.4.3, TR 0.1.0, C&C 1.1.2, Wild 1.1.0, T&T 1.0.0, NU 1.1.0, UA 1.2.1,
BB 1.1.0, V&P 2.7.0, Fluidlogged API 3.3.3). Scene scripts: `G:\VBPTest\showcase121\tools\scenes\tr*.py`.

**World rules:** `doDaylightCycle`, `doWeatherCycle`, `doMobSpawning`, `doFireTick` and `mobGriefing` are off and
`randomTickSpeed` is 0, so nothing wanders in, oxidizes, decays or burns. Set the time with `/time set <n>` before each
shot. Every mob is posed with `NoAI`, `Silent`, `PersistenceRequired` and `Invulnerable`. Fly (creative) to the `/tp`
spot and hold still; nothing is built under the cameras.

**Swamp and pale garden biomes are painted:** the bogged hollow is swampland and the resin and creaking groves are pale
garden, so grass, leaves and water take those colors.

## Scenes

| # | Scene | `/tp` (x y z yaw pitch) | Time, weather | Shows | Slot in `DESCRIPTION.md` |
| --- | --- | --- | --- | --- | --- |
| 1 | `tr1_breeze` | `/tp @p -461.5 22 -227.5 180 6` | any (underground) | A hand-built trial chamber hall: tuff bricks, copper columns, a grate ceiling with lit bulbs, a copper inlay round a breeze trial spawner. A breeze hovers over the inlay facing you with a wind charge just off its arm; a second breeze low at the back; the copper doors to the vault room blown open; a breeze rod on the floor. | **The breeze and the bogged**, after the first paragraph |
| 2 | `tr2_bogged` | `/tp @p -489.5 64 -313.5 118 6` | 23600, clear (dawn) | A swamp hollow: shallow pools with lily pads, swamp oaks hung with vines, mushrooms and blue orchids, a collapsed stilt hut. Two bogged with bows in the shallows; a sheared one by the hut with its red and brown mushrooms and the shears at its feet. | **The breeze and the bogged**, after the second paragraph |
| 3 | `tr3_vault` | `/tp @p -438.5 22 -236.5 180 14` | any (underground) | The vault room: a normal vault and an ominous vault in a grate recess, a trial key dropped on the step, an ominous trial key, an ominous bottle and a spare trial key on a spruce shelf, the heavy core on a chiseled tuff pedestal, a mace in a wall frame over a crafter, and a stand in copper armor holding a mace. | **Trial chambers**, second paragraph (vaults, keys); the same shot can also go under **The mace** |
| 4 | `tr4_golem` | `/tp @p -419.5 65 -230.5 180 12` | 5000, clear | A spruce barn: copper chests in all four weathering stages on the west wall, chests and framed barrels on the north wall. A fresh copper golem carries wheat from the copper chests toward the barrels, head turned to the door; a weathered one waits by the oxidized chest. Copper lanterns (no copper torches: they crash the 0.1.0 client, see below), a lightning rod on the roof. | **The copper golem**, after the first paragraph (before the tester's statue photo) |
| 5 | `tr5_palegarden` | `/tp @p -284.5 103 224.5 166 -6` (standing) | 13200, clear (dusk into night) | Close on the creaking, 4 blocks off, in front of its heart tree. A grove of hand-built pale oaks on pale moss: moss carpet, hanging moss, open eyeblossoms. An awake creaking heart in the nearest trunk; the creaking stands between the trees facing you, a second half behind a trunk. The woods around were turned pale so only pale oaks show behind. | **The pale garden**, after the second paragraph |
| 6 | `tr6_resin` | `/tp @p -311.5 98 229.5 108 6` | 4000, clear | Morning at the garden's west edge: a dormant creaking heart low in a pale oak, eight resin clumps grown round it, a half-fallen shrine of resin bricks, chiseled resin bricks, resin brick stairs, slabs and walls, blocks of resin at its foot. | **The pale garden**, last (after the resin sentence) |
| 7 | `tr7_ghast` | `/tp @p -427.5 66 -282.5 140 -6` | 1500, clear | A meadow pond: a dried ghast soaking in the shallows (fully hydrated), a dry one on the bank, the ghastling bobbing over the water. A grown happy ghast in a blue harness hovers beside a spruce boarding platform whose deck is level with its back (ladder up, fence rail). | **The happy ghast** |
| 8 | `tr8_parched` | `/tp @p -197.5 65 -564.5 92 2` | 10500, clear | Hand-shaped dunes and a half-buried sandstone ruin round a broken desert well; three parched with bows (one coming over the dune), cacti with cactus flowers, dead bushes, short and tall dry grass. | **Spears and the nautilus**, after the third paragraph (the parched sentence) |
| 9 | `tr9_horses` | `/tp @p -525.5 108 -232.5 168 4` | 12300, clear (sunset) | A savanna plateau at sunset: a zombie rides a zombie horse with an iron spear, two riderless zombie horses by an abandoned acacia lean-to, a zombie on foot with a stone spear. | **Spears and the nautilus**, with the parched/zombie horse paragraph (second image) |
| 10 | `tr10_spears` | `/tp @p -530.5 104 -250.5 160 14` | 11000, clear | The same lean-to close up: a copper spear in a frame on the post, a diamond spear on a weapon stand, a wooden spear dropped in the dry grass by the cold campfire. | **Spears and the nautilus**, after the first paragraph |
| 11 | `tr11_bundles` | `/tp @p -417.5 65 -235.5 -150 18` | 5000, clear | Inside the golem barn: orange, purple and cyan bundles on an oak shelf; red, yellow, lime, light blue and plain bundles as framed labels on the barrel wall among wheat, carrot and egg labels. | **Items and blocks**, after the list (bundles) |
| 12 | `tr12_effects` | `/tp @p -462.5 22 -209.5 122 12` | any (underground) | A chamber side room made into a brewery: behind copper bars a zombie with Infested, a husk with Wind Charged, a skeleton with Weaving in its own cobwebs and a slime with Oozing, all showing their particles. Brewing stands with the four potions, tipped arrows of Weaving and Oozing in frames, splash and lingering potions on a shelf. For the HUD icons too, run `/effect @p trickytrialsbackport:weaving 600` (and the others) before the shot. | **Items and blocks**, after the list (effects) |
| 13 | `tr13_paintings` | `/tp @p -403.5 65 -231.5 180 6` | 6000, clear | A small calcite gallery with spruce beams: prairie ride, backyard, orb and meditative on the west wall, unpacked, sunflowers and pond on the east wall, baroque, cavebird and humble on the end wall. | **Items and blocks**, after the list (paintings) |
| 14 | `tr14_nautilus` | `/tp @p 43.5 55 -451.5 172 6` | 6000, clear (underwater) | A hand-planted warm reef (coral heads, fans, sea pickles, seagrass, kelp). A tamed nautilus in diamond nautilus armor with a saddle, a wild one behind, a coral-grown zombie nautilus carrying a drowned with a trident, tropical fish. Breathe with `/effect @p water_breathing 600` while framing. | **Spears and the nautilus**, after the second paragraph |
| 15 | locator bar | any outdoor scene, e.g. `tr7_ghast` or `tr9_horses` | as that scene | **Needs a second player.** The bar only shows other players. Open the world to LAN and join from a second client (or connect two clients to `G:\VBPTest\showcase121`), stand the second player 20-60 blocks off to one side, then shoot with the HUD on (F1 off). | **The rest of 1.21**, first paragraph |
| 16 | settings screen | in game: Options, Mod Options, Backport Core, Tricky Trials | n/a | The settings screen. The user captures it; nothing to build. | **The rest of 1.21**, after "Every feature can be turned off" |

## Image lines to paste

When a picture is uploaded, add its line to `DESCRIPTION.md` at the slot above, for example:

```
![A breeze over a trial spawner, a wind charge just thrown](TODO-tr1_breeze.jpg)
![Two bogged and a sheared one in a swamp at dawn](TODO-tr2_bogged.jpg)
![A vault and an ominous vault, trial keys, the heavy core and the mace](TODO-tr3_vault.jpg)
![A copper golem carrying wheat between copper chests and barrels](TODO-tr4_golem.jpg)
![The creaking among pale oaks at night, its heart glowing in a trunk](TODO-tr5_palegarden.jpg)
![Resin clumps on a creaking heart's tree and a resin brick shrine](TODO-tr6_resin.jpg)
![A harnessed happy ghast by its boarding platform, a ghastling over a pond](TODO-tr7_ghast.jpg)
![Parched in a desert ruin](TODO-tr8_parched.jpg)
![A zombie with a spear riding a zombie horse on the savanna at sunset](TODO-tr9_horses.jpg)
![Spears at an abandoned savanna camp](TODO-tr10_spears.jpg)
![Bundles on a shelf and as barrel labels](TODO-tr11_bundles.jpg)
![Mobs with Infested, Wind Charged, Weaving and Oozing, and the new potions](TODO-tr12_effects.jpg)
![The new paintings in a gallery](TODO-tr13_paintings.jpg)
![A tamed nautilus in diamond armor and a zombie nautilus on a warm reef](TODO-tr14_nautilus.jpg)
![The locator bar showing another player](TODO-tr15_locator.jpg)
![Backport Core's settings screen with Tricky Trials' options](TODO-tr16_settings.jpg)
```

## Checks before shooting

- Paintings: ten on calcite; a painting with no solid wall behind it drops as an item. If one is missing, its spot was
  wrong; the gallery script is `tr13_paintings.py`.
- Vaults wake when you come within 4 blocks and cycle their loot; shoot from the `/tp` spot.
- The trial spawner waits for players; with `doMobSpawning` off it won't spawn breezes into the hall.
- Re-running a scene script rebuilds it in place (`/sch` helper on the build server, then copy the world again).
- **Copper torches crash TR 0.1.0's client** (`Invalid call to Particle.setMiscTex` from `CopperGearClient$CopperFlame`:
  it overrides `getFXLayer()` to 1, and `ParticleFlame`'s constructor then calls `setParticleTextureIndex`, which throws
  on any layer but 0). The showcase world has none; the barn uses copper lanterns instead.

## Upload folder

`E:\MinecraftInstances\BackportShowcase\minecraft\screenshots\curseforge\tricky-trials\`: 25 JPEGs (1920 x 1080, except the 1410 x 563 item overview),
numbered in page order. The tester's ten keepers (`PHOTOS.md`) are cropped to 16:9 past the launcher's "..." button
and scaled down; the rest are the showcase screenshots of 2026-11-21. Featured image: `02-tr-trial-spawner.jpg`.

01 chamber hall (Banner 19), 02 trial spawner (Banner 15), 03 chamber beds (Banner 18), 04 vault, keys and mace,
05 breeze, 06 bogged, 07 crafter (Banner 14), 08 copper interior (Banner 12), 09 copper pavilion (Banner 11),
10 copper golem, 11 copper golem statue (Banner 10), 12 pale garden and creaking, 13 resin, 14 leaf litter (Banner 4),
15 warm animals (Banner 2), 16 cold animals (Banner 9), 17 happy ghast, 18 nautilus, 19 parched,
20 zombie horses and spears, 21 every new item and block (an inventory overview), 22 bundles, 23 effects,
24 paintings, 25 settings. 12 is a later close-up of the creaking at its heart tree.

Not shot yet: the spear close-up (`tr10_spears`) and the locator bar.
