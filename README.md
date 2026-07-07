
Installation information
=======

This template repository can be directly cloned to get you started with a new
mod. Simply create a new repository cloned from this one, by following the
instructions provided by [GitHub](https://docs.github.com/en/repositories/creating-and-managing-repositories/creating-a-repository-from-a-template).

Once you have your clone, simply open the repository in the IDE of your choice. The usual recommendation for an IDE is either IntelliJ IDEA or Eclipse.

If at any point you are missing libraries in your IDE, or you've run into problems you can
run `gradlew --refresh-dependencies` to refresh the local cache. `gradlew clean` to reset everything 
{this does not affect your code} and then start the process again.

Mapping Names:
============
By default, the MDK is configured to use the official mapping names from Mojang for methods and fields 
in the Minecraft codebase. These names are covered by a specific license. All modders should be aware of this
license. For the latest license text, refer to the mapping file itself, or the reference copy here:
https://github.com/NeoForged/NeoForm/blob/main/Mojang.md

Additional Resources: 
==========
Community Documentation: https://docs.neoforged.net/  
NeoForged Discord: https://discord.neoforged.net/

## Space access commands

- `/genesis space` — jump straight into the Great Unknown from anywhere (any player, no OP needed).
- `/genesis land` — drop from space into the nearest planet's atmosphere (slow falling included).
- Flying or teleporting above y=2048 (`atmosphereExitHeight` in `genesis-common.toml`) also
  transfers you automatically; an actionbar countdown appears from ~y=1130 upward.

## Space travel loop

The full loop, all dimension hops hidden behind the frame-capture transition screen (no vanilla
loading screen), with player/craft momentum, rotation and passengers preserved:

1. **Earth -> space** — fly a Sable/Create Aeronautics craft (or yourself) above `atmosphereExitHeight`
   (default y=2048). The sky darkens and stars fade in on the climb (surface -> lower -> upper
   atmosphere -> near-space) via the planet atmosphere renderer, then you slip into the Great Unknown
   just off the cube face you launched from.
2. **Space** — the Great Unknown holds Earth, Moon, Sun and the black hole at their real positions.
   The Moon orbits ~10,000 blocks from Earth and grows as you approach.
3. **Space -> Moon** — come within the Moon's approach radius and you drop into the Moon dimension
   (`genesis:moon`) — its own dimension for gravity/terrain, entered seamlessly.
4. **Deep space** — stray past `deepSpaceRadius` (default 30,000 from Earth, i.e. ~20,000 past the
   Moon) and you cross into deep space (`genesis:subspace`).
5. **Return** — approach Earth and you re-enter the atmosphere; **which cube face you approach picks a
   distinct overworld region** (top / bottom / N / S / E / W never collapse to the same coordinates),
   and where on the face you approach shifts where in that region you land.

Re-entry / atmosphere-exit plasma heating applies to Sable physics objects only. All distances live
in `genesis-common.toml` under `[travel]`.
