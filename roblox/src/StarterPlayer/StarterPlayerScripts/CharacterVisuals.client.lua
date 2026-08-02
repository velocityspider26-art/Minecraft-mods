--!nonstrict
-- CharacterVisuals -- shared first/third-person presentation.
--
-- The shooter's camera viewmodel remains local for responsiveness. Every client
-- also builds a third-person copy of each OTHER player's current weapon and
-- evaluates the exact replicated reload variant/time. This avoids streaming
-- dozens of weapon parts over the network while making the weapon, hands,
-- muzzle event and reload choreography visible to everyone.

local Players = game:GetService("Players")
local ReplicatedStorage = game:GetService("ReplicatedStorage")
local RunService = game:GetService("RunService")

local Modules = ReplicatedStorage:WaitForChild("Modules")
local Weapons = require(Modules:WaitForChild("Weapons"))
local VM = require(Modules:WaitForChild("Viewmodel"))
local Animations = require(Modules:WaitForChild("Animations"))
local SoldierVisual = require(Modules:WaitForChild("SoldierVisual"))
local Cfg = require(Modules:WaitForChild("GameConfig"))

local localPlayer = Players.LocalPlayer
local records: {[Player]: any} = {}

local function weaponDef(id: any): any
	if typeof(id) == "string" and table.find(Weapons.ALL, id) then
		return Weapons[id]
	end
	return Weapons.MK18
end

local function destroyWeapon(rec: any)
	if rec.weaponRig then VM.destroy(rec.weaponRig) end
	rec.weaponRig = nil
	rec.weaponKey = nil
	rec.anim = nil
end

local function destroyRecord(plr: Player)
	local rec = records[plr]
	if not rec then return end
	destroyWeapon(rec)
	SoldierVisual.destroy(rec.body)
	for _, c in ipairs(rec.connections or {}) do c:Disconnect() end
	records[plr] = nil
end

local function buildWeapon(rec: any, id: string, suppressed: boolean)
	destroyWeapon(rec)
	local def = weaponDef(id)
	rec.weaponKey = id .. (suppressed and ":suppressed" or ":bare")
	rec.weaponRig = VM.build(def, suppressed, rec.character, {thirdPerson = true})
	rec.anim = Animations.newPlayer(def)
	rec.animSerial = -1
	rec.shotSerial = tonumber(rec.character:GetAttribute("WeaponShotSerial")) or 0
end

local function addCharacter(plr: Player, character: Model)
	destroyRecord(plr)
	local rec = {
		player = plr,
		character = character,
		body = SoldierVisual.build(character),
		weaponRig = nil,
		weaponKey = nil,
		anim = nil,
		animSerial = -1,
		shotSerial = -1,
		adsAlpha = 0,
		lean = 0,
		connections = {},
	}
	records[plr] = rec
	if plr ~= localPlayer then
		local id = tostring(character:GetAttribute("WeaponId") or "MK18")
		buildWeapon(rec, id, character:GetAttribute("WeaponSuppressed") == true)
	end
	table.insert(rec.connections, character.AncestryChanged:Connect(function(_, parent)
		if not parent then destroyRecord(plr) end
	end))
end

local function addPlayer(plr: Player)
	-- FPSClient owns the local player's real body, head camera and arm IK.
	-- CharacterVisuals is only the replicated presentation for OTHER players.
	if plr == localPlayer then return end
	if plr.Character then task.defer(addCharacter, plr, plr.Character) end
	plr.CharacterAdded:Connect(function(char) task.defer(addCharacter, plr, char) end)
end

for _, plr in ipairs(Players:GetPlayers()) do addPlayer(plr) end
Players.PlayerAdded:Connect(addPlayer)
Players.PlayerRemoving:Connect(destroyRecord)

local function smooth(current: number, target: number, speed: number, dt: number): number
	return current + (target - current) * (1 - math.exp(-speed * math.min(dt, 1/20)))
end

-- Build the third-person weapon from the actual stock/shoulder relationship,
-- not a guessed root offset. `def.pocket` is the butt-pad position in weapon
-- local space. Solving the root from that point guarantees the stock remains
-- in the shoulder while hip, ADS, pitch and lean change.
local function remoteBaseCF(character: Model, def: any, stance: string, adsAlpha: number,
	pitch: number, lean: number): (CFrame?, CFrame?)
	local torso = character:FindFirstChild("UpperTorso") or character:FindFirstChild("Torso")
	if not torso or not torso:IsA("BasePart") then return nil, nil end
	local chestCF = torso.CFrame
	local chestRot = chestCF - chestCF.Position
	local pocket = def.pocket or Vector3.new(0, .30, .80)

	if stance == "sprint" then
		local sprint = chestCF
			* CFrame.new(.34, -.06, -.30)
			* CFrame.Angles(math.rad(-35) + pitch * .30, math.rad(21), math.rad(9))
		return sprint, chestCF
	end

	local a = math.clamp(adsAlpha, 0, 1)
	local crouch = stance == "crouch" and 1 or 0
	-- Shoulder pocket stays behind the right pectoral. ADS slightly raises and
	-- pulls it inward rather than translating the entire rifle through the body.
	local shoulderLocal = Vector3.new(
		.38 - .055 * a + .075 * lean,
		.30 + .035 * a - .025 * crouch,
		-.055 - .018 * a
	)
	local shoulderWorld = chestCF:PointToWorldSpace(shoulderLocal)

	local pitchBias = math.rad(-7.0 + 5.4 * a - 1.8 * crouch)
	local yawBias = math.rad(-6.0 + 5.2 * a)
	local rollBias = math.rad(2.2 * (1 - a) - 3.5 * lean)
	local orientation = chestRot
		* CFrame.Angles(pitchBias + pitch * (.57 + .12 * a), yawBias, rollBias)

	local rootPos = shoulderWorld - orientation:VectorToWorldSpace(pocket)
	local root = CFrame.new(rootPos) * orientation
	return root, chestCF
end

RunService:BindToRenderStep("BlacksiteCharacterVisuals", Enum.RenderPriority.Camera.Value + 2, function(dt)
	local cam = workspace.CurrentCamera
	if not cam then return end
	for plr, rec in pairs(records) do
		local char = rec.character
		if not char or not char.Parent then continue end
		local leanTarget = math.clamp(tonumber(char:GetAttribute("WeaponLean")) or 0, -1, 1)
		rec.lean = smooth(rec.lean or 0, leanTarget, 14, dt)

		local id = tostring(char:GetAttribute("WeaponId") or "MK18")
		local suppressed = char:GetAttribute("WeaponSuppressed") == true
		local key = id .. (suppressed and ":suppressed" or ":bare")
		if not rec.weaponRig or rec.weaponKey ~= key then
			buildWeapon(rec, id, suppressed)
		end
		local rig, playerAnim = rec.weaponRig, rec.anim
		if not rig or not playerAnim then continue end

		local serial = tonumber(char:GetAttribute("WeaponAnimSerial")) or 0
		local name = tostring(char:GetAttribute("WeaponAnimName") or "")
		if serial ~= rec.animSerial then
			rec.animSerial = serial
			if name ~= "" then
				playerAnim:playResolved(name)
			else
				playerAnim:stop()
			end
		end
		local netTime = tonumber(char:GetAttribute("WeaponAnimTime")) or 0
		local netScale = tonumber(char:GetAttribute("WeaponAnimScale")) or 1
		if playerAnim.anim then
			-- Correct gently toward the replicated authored time. This preserves
			-- continuity between 20 Hz packets while preventing hand/mag drift.
			local predicted = playerAnim.time + dt * playerAnim.scale
			playerAnim.time = predicted + (netTime - predicted) * math.clamp(dt * 14, 0, 1)
			playerAnim.scale = netScale
		end
		local pose = playerAnim:update(dt)

		local stance = tostring(char:GetAttribute("WeaponStance") or "still")
		local adsTarget = char:GetAttribute("WeaponADS") == true and 1 or 0
		rec.adsAlpha = smooth(rec.adsAlpha or 0, adsTarget, 11.5, dt)
		local pitch = tonumber(char:GetAttribute("LookPitch")) or 0
		local rootPart = char:FindFirstChild("HumanoidRootPart")
		local torsoPart = char:FindFirstChild("UpperTorso") or char:FindFirstChild("Torso")
		local hum = char:FindFirstChildOfClass("Humanoid")
		local worldVel = rootPart and rootPart:IsA("BasePart") and rootPart.AssemblyLinearVelocity or Vector3.zero
		local localVel = torsoPart and torsoPart:IsA("BasePart")
			and torsoPart.CFrame:VectorToObjectSpace(worldVel) or Vector3.zero
		local humState = hum and hum:GetState() or Enum.HumanoidStateType.Running
		local airborne = hum and (hum.FloorMaterial == Enum.Material.Air
			or humState == Enum.HumanoidStateType.Jumping
			or humState == Enum.HumanoidStateType.Freefall) or false
		SoldierVisual.poseBody(rec.body, {
			dt = dt,
			lean = rec.lean,
			torsoPitch = pitch * .22,
			headPitch = pitch,
			stance = stance == "crouch" and 1 or 0,
			speed = math.clamp(Vector3.new(worldVel.X, 0, worldVel.Z).Magnitude / Cfg.MOVE.WALK, 0, 1.35),
			sprint = stance == "sprint",
			ads = rec.adsAlpha,
			moveX = math.clamp(localVel.X / math.max(Cfg.MOVE.WALK, .01), -1, 1),
			moveZ = math.clamp(localVel.Z / math.max(Cfg.MOVE.WALK, .01), -1, 1),
			airborne = airborne,
			verticalVelocity = worldVel.Y,
		})
		local rootCF, chestCF = remoteBaseCF(char, weaponDef(id), stance, rec.adsAlpha, pitch, rec.lean)
		if rootCF and chestCF then
			local weaponWorldCF, handL, handR = VM.update(rig, rootCF, chestCF, pose)
			SoldierVisual.poseArms(rec.body, chestCF, handL, handR, pose,
				weaponWorldCF, weaponDef(id))
		end
		-- Sync an imported custom mesh after all Motor6D posing so its arms, torso
		-- and head read the final skeleton transforms from this frame.
		SoldierVisual.updateBody(rec.body, cam.CFrame, false)

		local shot = tonumber(char:GetAttribute("WeaponShotSerial")) or 0
		if shot ~= rec.shotSerial then
			rec.shotSerial = shot
			VM.pulseMuzzle(rig, suppressed)
		end
	end
end)
