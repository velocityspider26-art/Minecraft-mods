--!nonstrict
-- Anatomy -- server-authoritative anatomical trauma simulation.
--
-- This is a game model, not a medical simulator. It deliberately avoids fake
-- millimetre-perfect claims while tracking the things the player can actually
-- perceive and act on: tissue trauma, named bones, organ damage, external and
-- internal bleeding, blood volume, pain, shock and functional impairment.

local Anatomy = {}

Anatomy.BLOOD_MAX = 5000 -- millilitres, used as a readable game-scale pool
Anatomy.BLOOD_FATAL = 1650

Anatomy.BONES = {
	Skull={max=90, limb="Head", label="skull"}, Jaw={max=48, limb="Head", label="jaw"},
	CervicalSpine={max=78, limb="Torso", label="cervical spine"},
	ThoracicSpine={max=92, limb="Torso", label="thoracic spine"},
	LumbarSpine={max=96, limb="Torso", label="lumbar spine"},
	Sternum={max=64, limb="Torso", label="sternum"},
	LeftRibs={max=52, limb="Torso", label="left ribs"}, RightRibs={max=52, limb="Torso", label="right ribs"},
	Pelvis={max=118, limb="Torso", label="pelvis"},
	LeftClavicle={max=50, limb="LeftArm", label="left clavicle"}, RightClavicle={max=50, limb="RightArm", label="right clavicle"},
	LeftHumerus={max=76, limb="LeftArm", label="left humerus"}, RightHumerus={max=76, limb="RightArm", label="right humerus"},
	LeftRadius={max=56, limb="LeftArm", label="left radius"}, RightRadius={max=56, limb="RightArm", label="right radius"},
	LeftUlna={max=60, limb="LeftArm", label="left ulna"}, RightUlna={max=60, limb="RightArm", label="right ulna"},
	LeftHandBones={max=42, limb="LeftArm", label="left hand"}, RightHandBones={max=42, limb="RightArm", label="right hand"},
	LeftFemur={max=108, limb="LeftLeg", label="left femur"}, RightFemur={max=108, limb="RightLeg", label="right femur"},
	LeftPatella={max=44, limb="LeftLeg", label="left patella"}, RightPatella={max=44, limb="RightLeg", label="right patella"},
	LeftTibia={max=88, limb="LeftLeg", label="left tibia"}, RightTibia={max=88, limb="RightLeg", label="right tibia"},
	LeftFibula={max=58, limb="LeftLeg", label="left fibula"}, RightFibula={max=58, limb="RightLeg", label="right fibula"},
	LeftFootBones={max=48, limb="LeftLeg", label="left foot"}, RightFootBones={max=48, limb="RightLeg", label="right foot"},
}

Anatomy.ORGANS = {
	Brain={max=100, label="brain"}, Heart={max=100, label="heart"},
	LeftLung={max=100, label="left lung"}, RightLung={max=100, label="right lung"},
	Liver={max=100, label="liver"}, LeftKidney={max=100, label="left kidney"}, RightKidney={max=100, label="right kidney"},
}

Anatomy.BONE_KEYS = {}
for key in Anatomy.BONES do table.insert(Anatomy.BONE_KEYS, key) end
table.sort(Anatomy.BONE_KEYS)

local function fractureLevel(value: number, maximum: number): number
	local ratio = value / math.max(maximum, 1)
	if ratio <= 0.03 then return 3 end -- shattered / nonfunctional
	if ratio <= 0.34 then return 2 end -- complete fracture
	if ratio <= 0.66 then return 1 end -- crack / incomplete fracture
	return 0
end
Anatomy.fractureLevel = fractureLevel

function Anatomy.newState()
	local state = {
		blood = Anatomy.BLOOD_MAX,
		pain = 0,
		shock = 0,
		bones = {},
		organs = {},
		bleeds = {}, -- [source] = {rate, external}
		lastBone = "",
		lastOrgan = "",
		woundSerial = 0,
	}
	for key, def in Anatomy.BONES do state.bones[key] = def.max end
	for key, def in Anatomy.ORGANS do state.organs[key] = def.max end
	return state
end

local function normalizedHit(part: BasePart, worldPosition: Vector3): Vector3
	local p = part.CFrame:PointToObjectSpace(worldPosition)
	local h = part.Size * 0.5
	return Vector3.new(
		math.clamp(p.X / math.max(h.X, .01), -1, 1),
		math.clamp(p.Y / math.max(h.Y, .01), -1, 1),
		math.clamp(p.Z / math.max(h.Z, .01), -1, 1)
	)
end

local function sidePrefix(name: string): string
	if string.find(name, "Left", 1, true) then return "Left" end
	if string.find(name, "Right", 1, true) then return "Right" end
	if string.find(name, "Left ", 1, true) then return "Left" end
	return "Right"
end

function Anatomy.resolve(part: BasePart, worldPosition: Vector3)
	local name = part.Name
	local n = normalizedHit(part, worldPosition)
	local limb, bone, organ = "Torso", "Sternum", nil

	if name == "Head" then
		limb = "Head"
		bone = n.Y < -0.38 and "Jaw" or "Skull"
		organ = n.Y > -0.30 and "Brain" or nil
	elseif name == "UpperTorso" or name == "Torso" then
		limb = "Torso"
		if n.Z > 0.42 and math.abs(n.X) < 0.36 then
			bone = "ThoracicSpine"
		elseif n.Y > 0.48 and math.abs(n.X) > 0.34 then
			bone = n.X < 0 and "LeftClavicle" or "RightClavicle"
		elseif math.abs(n.X) < 0.22 and n.Z < 0.18 then
			bone = "Sternum"
		else
			bone = n.X < 0 and "LeftRibs" or "RightRibs"
		end
		if n.Z < 0.48 then
			if math.abs(n.X) < 0.20 and n.Y > -0.22 and n.Y < 0.30 then
				organ = "Heart"
		else
				organ = n.X < 0 and "LeftLung" or "RightLung"
			end
		end
	elseif name == "LowerTorso" then
		limb = "Torso"
		if n.Z > 0.40 and math.abs(n.X) < 0.38 then
			bone = "LumbarSpine"
		else
			bone = "Pelvis"
		end
		if n.Y > -0.10 then
			if n.Z > 0.25 then organ = n.X < 0 and "LeftKidney" or "RightKidney"
			elseif n.X > 0.10 then organ = "Liver" end
		end
	elseif string.find(name, "UpperArm", 1, true) or name == "Left Arm" or name == "Right Arm" then
		local s = sidePrefix(name); limb = s.."Arm"; bone = s.."Humerus"
	elseif string.find(name, "LowerArm", 1, true) then
		local s = sidePrefix(name); limb = s.."Arm"; bone = s..(n.X < 0 and "Radius" or "Ulna")
	elseif string.find(name, "Hand", 1, true) then
		local s = sidePrefix(name); limb = s.."Arm"; bone = s.."HandBones"
	elseif string.find(name, "UpperLeg", 1, true) or name == "Left Leg" or name == "Right Leg" then
		local s = sidePrefix(name); limb = s.."Leg"; bone = s.."Femur"
	elseif string.find(name, "LowerLeg", 1, true) then
		local s = sidePrefix(name); limb = s.."Leg"
		if n.Y > 0.70 then bone = s.."Patella" else bone = s..(n.X < 0 and "Tibia" or "Fibula") end
	elseif string.find(name, "Foot", 1, true) then
		local s = sidePrefix(name); limb = s.."Leg"; bone = s.."FootBones"
	end
	return limb, bone, organ, n
end

local BONE_EXPOSURE = {
	Head=1.85, Torso=1.00, LeftArm=1.75, RightArm=1.75, LeftLeg=1.55, RightLeg=1.55,
}
local TISSUE_BLEED = {
	Head=1.35, Torso=1.15, LeftArm=.85, RightArm=.85, LeftLeg=1.00, RightLeg=1.00,
}
local FRACTURE_BLEED = {
	Skull=4, Jaw=2, CervicalSpine=5, ThoracicSpine=6, LumbarSpine=6,
	Sternum=3, LeftRibs=3, RightRibs=3, Pelvis=18,
	LeftClavicle=2, RightClavicle=2, LeftHumerus=5, RightHumerus=5,
	LeftRadius=3, RightRadius=3, LeftUlna=3, RightUlna=3,
	LeftHandBones=1, RightHandBones=1,
	LeftFemur=22, RightFemur=22, LeftPatella=2, RightPatella=2,
	LeftTibia=9, RightTibia=9, LeftFibula=5, RightFibula=5,
	LeftFootBones=2, RightFootBones=2,
}
local ORGAN_BLEED = {Brain=8, Heart=55, LeftLung=16, RightLung=16, Liver=24, LeftKidney=14, RightKidney=14}

local function addBleed(state, source: string, rate: number, external: boolean)
	if rate <= .05 then return end
	local old = state.bleeds[source]
	if old then
		old.rate = math.max(old.rate, rate)
	else
		state.bleeds[source] = {rate=rate, external=external}
	end
end

function Anatomy.totalBleed(state): (number, number, number)
	local total, external, internal = 0, 0, 0
	for _, bleed in state.bleeds do
		total += bleed.rate
		if bleed.external then external += bleed.rate else internal += bleed.rate end
	end
	return total, external, internal
end

function Anatomy.applyHit(state, part: BasePart, worldPosition: Vector3,
	tissueDamage: number, ballisticTrauma: number, random01: number?)
	local limb, bone, organ = Anatomy.resolve(part, worldPosition)
	local variance = .88 + (random01 or math.random()) * .24
	local result = {limb=limb, bone=bone, organ=organ, fracture=0, fractureChanged=false,
		organCritical=false, organChanged=false, immediateDamage=tissueDamage, fatal=false}

	local boneDef = Anatomy.BONES[bone]
	if boneDef then
		local before = state.bones[bone] or boneDef.max
		local oldLevel = fractureLevel(before, boneDef.max)
		local boneDamage = ballisticTrauma * (BONE_EXPOSURE[limb] or 1) * variance
		local after = math.max(0, before - boneDamage)
		state.bones[bone] = after
		local newLevel = fractureLevel(after, boneDef.max)
		result.fracture = newLevel
		if newLevel > oldLevel then
			state.lastBone = boneDef.label
			result.fractureChanged = true
			addBleed(state, "bone:"..bone, (FRACTURE_BLEED[bone] or 2) * newLevel, false)
		end
		result.immediateDamage += newLevel * 1.5
	end

	if organ and Anatomy.ORGANS[organ] then
		local def = Anatomy.ORGANS[organ]
		local before = state.organs[organ] or def.max
		local organMult = (organ == "Heart" or organ == "Brain") and 2.0 or 1.20
		local organDamage = ballisticTrauma * organMult * variance
		local after = math.max(0, before - organDamage)
		state.organs[organ] = after
		state.lastOrgan = def.label
		result.organChanged = after < before
		addBleed(state, "organ:"..organ, (ORGAN_BLEED[organ] or 8) * math.clamp(organDamage / 30, .4, 2.0), false)
		if after <= def.max * .35 then result.organCritical = true end
		if after <= 0 and (organ == "Brain" or organ == "Heart") then result.fatal = true end
		result.immediateDamage += organDamage * .18
	end

	local externalRate = tissueDamage * (TISSUE_BLEED[limb] or 1) * (.55 + variance * .35)
	state.woundSerial = (state.woundSerial or 0) + 1
	addBleed(state, "wound:"..limb..":"..tostring(state.woundSerial), externalRate, true)
	state.pain = math.clamp(state.pain + tissueDamage * 1.25 + result.fracture * 10, 0, 100)
	if bone == "CervicalSpine" and result.fracture >= 3 then result.fatal = true end
	return result
end

function Anatomy.applyDressing(state)
	local worstKey, worstRate = nil, 0
	for key, bleed in state.bleeds do
		if bleed.external and bleed.rate > worstRate then
			worstKey, worstRate = key, bleed.rate
		end
	end
	if not worstKey then return false, 0 end
	local bleed = state.bleeds[worstKey]
	local stopped = bleed.rate * .88
	bleed.rate *= .12
	if bleed.rate < .25 then state.bleeds[worstKey] = nil end
	state.pain = math.max(0, state.pain - 6)
	return true, stopped
end

function Anatomy.tick(state, dt: number)
	local total = Anatomy.totalBleed(state)
	state.blood = math.max(0, state.blood - total * dt)
	state.pain = math.max(0, state.pain - .20 * dt)
	local bloodShock = math.clamp((3600 - state.blood) / 2100, 0, 1)
	local painShock = math.clamp(state.pain / 100, 0, 1) * .42
	state.shock += (math.max(bloodShock, painShock) - state.shock) * math.min(1, dt * 2.2)
	local healthLoss = 0
	if state.blood < 3300 then
		healthLoss = ((3300 - state.blood) / 1650) * 2.5 * dt
	end
	local fatal = state.blood <= Anatomy.BLOOD_FATAL
	return healthLoss, fatal
end

function Anatomy.functionalPenalties(state)
	local function level(key)
		local def = Anatomy.BONES[key]
		return def and fractureLevel(state.bones[key] or def.max, def.max) or 0
	end
	local leftLeg = math.max(level("LeftFemur"), level("LeftTibia"), level("LeftFibula"), level("LeftPatella"), level("LeftFootBones"))
	local rightLeg = math.max(level("RightFemur"), level("RightTibia"), level("RightFibula"), level("RightPatella"), level("RightFootBones"))
	local leftArm = math.max(level("LeftClavicle"), level("LeftHumerus"), level("LeftRadius"), level("LeftUlna"), level("LeftHandBones"))
	local rightArm = math.max(level("RightClavicle"), level("RightHumerus"), level("RightRadius"), level("RightUlna"), level("RightHandBones"))
	local axial = math.max(level("Pelvis"), level("CervicalSpine"), level("ThoracicSpine"), level("LumbarSpine"))
	return {leftLeg=leftLeg, rightLeg=rightLeg, leftArm=leftArm, rightArm=rightArm, axial=axial}
end

return Anatomy
