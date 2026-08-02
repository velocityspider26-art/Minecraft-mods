--!nonstrict
-- HumanArms -- puts the soldier's hands ON the weapon, not near it.
--
-- The old viewmodel arms were deliberately not anatomical: shoulders parked
-- behind the camera and bones long enough to reach whatever the weapon needed.
-- That works for a pair of floating first-person arms and does not work at all
-- for a real body seen in third person, where the arm is attached to a real
-- clavicle at a real height and is exactly as long as it is.
--
-- So the contact is solved from both ends instead of authored:
--
--   the WEAPON supplies a grip frame, measured off the geometry of the pistol
--   grip and the foregrip (KSVRMesh.GRIPS);
--   the HAND supplies a palm frame, measured off the knuckle bones of the
--   actual rig (SoldierRigData.HANDS);
--
-- and the wrist goes wherever those two frames coincide:
--
--   wrist = weaponCF * weaponGrip * palm:Inverse()
--
-- Because both frames carry a full orientation and not just a point, the
-- fingers close along the grip rather than through it. The remaining job is a
-- two-bone solve for the elbow, which is what this module does.
--
-- The offline previewer roblox/tools/preview_pose.py runs this identical
-- construction in numpy and renders it; palm-to-grip error there is 0.0000.

local HumanRig = require(script.Parent:WaitForChild("HumanRig"))
local Cfg = require(script.Parent:WaitForChild("GameConfig"))

local HumanArms = {}

-- Minimal rotation carrying unit vector `a` onto unit vector `b`. Used instead
-- of building a basis so that each bone keeps the roll it has in the bind pose
-- -- rebuilding a basis from scratch twists the mesh around the limb axis.
local function swing(a: Vector3, b: Vector3): CFrame
	local dot = math.clamp(a:Dot(b), -1, 1)
	local axis = a:Cross(b)
	if axis.Magnitude < 1e-6 then
		if dot > 0 then return CFrame.identity end
		-- Opposed: any perpendicular axis gives the half turn.
		local perp = a:Cross(Vector3.xAxis)
		if perp.Magnitude < 1e-4 then perp = a:Cross(Vector3.zAxis) end
		return CFrame.fromAxisAngle(perp.Unit, math.pi)
	end
	return CFrame.fromAxisAngle(axis.Unit, math.atan2(axis.Magnitude, dot))
end

-- Elbow position for a two-bone chain, bending toward `pole`.
local function solveElbow(shoulder: Vector3, wrist: Vector3,
	l1: number, l2: number, pole: Vector3): Vector3
	local span = wrist - shoulder
	local dist = math.clamp(span.Magnitude, math.abs(l1 - l2) + 1e-3, l1 + l2 - 1e-3)
	local dir = span.Magnitude > 1e-6 and span.Unit or Vector3.new(0, -1, 0)
	local cosA = math.clamp((l1 * l1 + dist * dist - l2 * l2) / (2 * l1 * dist), -1, 1)
	local a = math.acos(cosA)
	local side = dir:Cross(pole)
	if side.Magnitude < 1e-4 then side = dir:Cross(Vector3.yAxis) end
	if side.Magnitude < 1e-4 then side = dir:Cross(Vector3.xAxis) end
	local bend = side.Unit:Cross(dir)
	if bend.Magnitude < 1e-4 then return shoulder + dir * l1 end
	return shoulder + (dir * math.cos(a) + bend.Unit * math.sin(a)) * l1
end

HumanArms.swing = swing
HumanArms.solveElbow = solveElbow

-- `wristTarget` is a world CFrame. Returns the wrist CFrame actually reached,
-- which differs from the target only when the grip is out of reach.
function HumanArms.solveTo(rig: any, side: string, wristTarget: CFrame, poleLocal: Vector3?): CFrame
	local armName = side .. "Arm"
	local foreName = side .. "ForeArm"
	local handName = side .. "Hand"

	local arm = rig.bones[armName]
	if not arm then return wristTarget end

	local rootRot = rig.root.Rotation
	-- This rig's own bind, not the shared one: it carries the fit-to-R15 scale,
	-- so the reach clamp and the elbow solve use the arm's real length.
	local fore = rig.bones[foreName]
	local hand = rig.bones[handName]
	if not fore or not hand then return wristTarget end
	local bindArm = arm.bind
	local bindFore = fore.bind
	local bindHand = hand.bind

	local l1 = (bindFore.Position - bindArm.Position).Magnitude
	local l2 = (bindHand.Position - bindFore.Position).Magnitude

	-- The shoulder has already been moved by the clavicle pose this frame.
	local shoulder = arm.world.Position
	local wrist = wristTarget.Position

	-- Never let the hand leave the arm. If the grip really is out of reach the
	-- arm straightens and the hand stops at full extension: a grip held a
	-- couple of centimetres short still reads as a grip, a hand floating off
	-- the end of a forearm does not.
	local span = wrist - shoulder
	local reach = (l1 + l2) * 0.995
	if span.Magnitude > reach then
		wrist = shoulder + span.Unit * reach
		wristTarget = wristTarget - wristTarget.Position + wrist
	end

	local pole = rootRot:VectorToWorldSpace(poleLocal or Vector3.new(0, -1, 0.3))
	local elbow = solveElbow(shoulder, wrist, l1, l2, pole)

	local bindArmDir = rootRot:VectorToWorldSpace((bindFore.Position - bindArm.Position).Unit)
	local armDir = (elbow - shoulder)
	if armDir.Magnitude > 1e-5 then
		local rot = swing(bindArmDir, armDir.Unit) * (rootRot * bindArm.Rotation)
		HumanRig.setWorld(rig, armName, CFrame.new(shoulder) * rot.Rotation)
	end

	local bindForeDir = rootRot:VectorToWorldSpace((bindHand.Position - bindFore.Position).Unit)
	local foreDir = (wrist - elbow)
	if foreDir.Magnitude > 1e-5 then
		local rot = swing(bindForeDir, foreDir.Unit) * (rootRot * bindFore.Rotation)
		HumanRig.setWorld(rig, foreName, CFrame.new(elbow) * rot.Rotation)
	end

	HumanRig.setWorld(rig, handName, wristTarget)
	return wristTarget
end

-- Solve both arms onto a weapon. `weaponCF` is the weapon root in world space;
-- `grips` is KSVRMesh.GRIPS (or any weapon's equivalent); `hands` is
-- SoldierRigData.HANDS.
function HumanArms.holdWeapon(rig: any, weaponCF: CFrame, grips: any, hands: any, opts: any?)
	opts = opts or {}
	local H = Cfg.HUMAN_ANIM
	local ads = math.clamp(tonumber(opts.ads) or 0, 0, 1)

	for _, side in ipairs({"Left", "Right"}) do
		local key = side == "Left" and "left" or "right"
		local grip = grips and grips[key]
		local palm = hands and hands[side]
		if grip and palm then
			local trim = opts[key .. "Trim"]
			local target = weaponCF * grip * (trim or CFrame.identity) * palm:Inverse()

			-- Elbow placement is what separates a shooting stance from a
			-- mannequin. The support elbow drops under the handguard; the
			-- firing elbow stays lower and closer to the ribs when braced and
			-- flares out as the weapon comes down off the sights.
			local pole
			if side == "Left" then
				pole = H.POLE_SUPPORT:Lerp(H.POLE_SUPPORT_ADS, ads)
			else
				pole = H.POLE_FIRING:Lerp(H.POLE_FIRING_ADS, ads)
			end
			HumanArms.solveTo(rig, side, target, pole)
		end
	end
end

return HumanArms
