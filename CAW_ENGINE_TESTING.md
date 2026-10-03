# Create Aerial Warfare — CAW Core v0.1 test notes

## Engine order

Build a straight line with every component facing the same airflow direction:

`[optional inlet] -> fan -> compressor -> combustor -> turbine -> nozzle`

The inlet is optional. When present it currently gives a 15% thrust bonus.

## Power

Apply redstone power to any block in the connected engine. The strongest signal anywhere in the chain is used as analog throttle:

- 0 = off
- 1..14 = partial throttle
- 15 = full throttle

There is no fuel requirement in v0.1.

## Spool

The engine does not jump instantly to commanded power. It takes about four seconds to spool from idle to full and about two seconds to spool down.

## Physics

When the engine is inside a compatible Sable ServerSubLevel, CAW resolves a Sable rigid-body handle at runtime and applies the per-tick impulse at the nozzle position. This means engine placement can naturally create torque when thrust is off-center.

## Visuals

The current block models are intentional placeholders while the previously designed Blockbench assets are wired in. Runtime cloud/smoke particles are also temporary; the final exhaust renderer will be upgraded later.
