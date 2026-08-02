--!nonstrict
-- ScreenFx -- shared pulse state for the full-screen effects.
--
-- FPSClient knows when you fire; Atmosphere owns the post-processing. They are
-- separate client scripts, so this module is the seam: requiring the same
-- ModuleScript from both gives the same table, FPSClient pokes values in, and
-- Atmosphere decays and applies them every frame.
--
-- Everything is a PULSE: set it to 1 and it falls off. Nothing here is a target
-- to be lerped toward, because a decaying impulse reads as an event and a lerped
-- target reads as a setting being changed -- the same reason the weapon springs
-- are velocity-seeded.

local Cfg = require(script.Parent:WaitForChild("GameConfig"))

local Fx = {
	fire = 0, -- your own muzzle blast
	hit = 0, -- you were hit
	suppress = 0, -- a round went past close enough to hear
	deafen = 0, -- accumulated muzzle blast; drives the low-pass and the ring
}

function Fx.pulse(kind: string, amount: number)
	local v = Fx[kind]
	if v ~= nil then
		-- Takes the max rather than accumulating, so a long burst does not
		-- multiply into an unreadable smear.
		Fx[kind] = math.min(1, math.max(v, amount))
	end
end

-- Muzzle blast: a short sharp pulse, plus a slower accumulating deafness that
-- only fully clears if you stop shooting.
function Fx.fired(weight: number)
	local F = Cfg.SCREENFX
	Fx.pulse("fire", F.FIRE_PULSE * weight)
	Fx.deafen = math.min(1, Fx.deafen + F.DEAFEN_PER_SHOT * weight)
end

function Fx.decay(dt: number)
	local F = Cfg.SCREENFX
	Fx.fire = math.max(0, Fx.fire - dt / F.FIRE_DECAY)
	Fx.hit = math.max(0, Fx.hit - dt / F.HIT_DECAY)
	Fx.suppress = math.max(0, Fx.suppress - dt / F.SUPPRESS_DECAY)
	Fx.deafen = math.max(0, Fx.deafen - dt / F.DEAFEN_DECAY)
end

return Fx
