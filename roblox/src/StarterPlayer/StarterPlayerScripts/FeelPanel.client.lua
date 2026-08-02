--!nonstrict
-- FeelPanel -- press F4. Everything about how the game feels, on screen, live.
--
-- WHY THIS EXISTS
--
-- Feel cannot be tuned by correspondence. Four rounds went "it still doesn't
-- feel like ARMA" -> guess at a constant -> ship -> "still wrong", which is a
-- two-day loop around a decision that takes about four seconds to make with the
-- gun in your hands. Nobody can name the number they want; everybody can tell
-- instantly when the gun swings too much.
--
-- So the numbers moved to where the judgement is. Every knob here is read live
-- by FPSClient out of GameConfig -- the same table this panel writes into --
-- so a change lands on the very next frame. Nothing needs recompiling, nothing
-- needs republishing, and there is no round trip.
--
-- The top block is diagnostics for the same reason: "the body is not there" is
-- not something the player should have to open the Output window to describe.
-- It says where the body is, how much of it is on screen, and whether the
-- skeleton solved -- which are the three questions worth asking.
--
-- Presets are the fastest way to find out what you actually want: flip between
-- ORIGINAL and MILSIM while turning and firing, and the difference is the thing
-- you have been trying to describe in words.

local Players = game:GetService("Players")
local UserInputService = game:GetService("UserInputService")
local RunService = game:GetService("RunService")
local ReplicatedStorage = game:GetService("ReplicatedStorage")

local player = Players.LocalPlayer
local Modules = ReplicatedStorage:WaitForChild("Modules")
local Cfg = require(Modules:WaitForChild("GameConfig"))

--------------------------------------------------------------------------------
-- KNOBS
--
-- `tbl`/`key` address GameConfig directly. `hint` is what the knob does in the
-- language of the thing you can see, not the language of the implementation --
-- "how far the gun swings when you whip the camera" is tunable, "ROT_ACCEL" is
-- not.
--------------------------------------------------------------------------------
local KNOBS = {
	{group = "WEAPON WEIGHT"},
	{tbl = "INERTIA", key = "ROT_ACCEL", label = "Swing when you turn",
		step = 0.002, min = 0, max = 0.06, fmt = "%.3f",
		hint = "the ARMA signature -- the muzzle lags the camera and catches up"},
	{tbl = "INERTIA", key = "MAX_ROT", label = "Swing limit",
		step = 0.02, min = 0.05, max = 0.80, fmt = "%.2f",
		hint = "how far it is allowed to lag before it stops"},
	{tbl = "INERTIA", key = "POS_ACCEL", label = "Shift when you accelerate",
		step = 0.001, min = 0, max = 0.03, fmt = "%.3f",
		hint = "the gun shoves back in your hands when you start and stop"},
	{tbl = "INERTIA", key = "ROT_D", label = "Settle (lower = floatier)",
		step = 0.5, min = 3, max = 26, fmt = "%.1f",
		hint = "how quickly the swing stops wobbling"},

	{group = "SWAY"},
	{tbl = "CAM", key = "BREATH_AMPLITUDE_HIP", label = "Sway at the hip",
		step = 0.002, min = 0, max = 0.08, fmt = "%.3f",
		hint = "you cannot hold a rifle still"},
	{tbl = "CAM", key = "BREATH_AMPLITUDE_ADS", label = "Sway on the sights",
		step = 0.001, min = 0, max = 0.05, fmt = "%.4f",
		hint = "hold right mouse and shift to steady it"},
	{tbl = "INERTIA", key = "IDLE_AMP", label = "Idle drift",
		step = 0.002, min = 0, max = 0.08, fmt = "%.3f",
		hint = "slow wander when you are stood still"},

	{group = "RECOIL"},
	{tbl = "RECOIL", key = "RISE_IMPULSE", label = "Kick",
		step = 3, min = 10, max = 140, fmt = "%.0f",
		hint = "how hard each round hits"},
	{tbl = "RECOIL", key = "PERMANENT", label = "Climb you have to pull down",
		step = 0.02, min = 0, max = 0.80, fmt = "%.2f",
		hint = "0 = it recentres itself, high = you fight it. This is the one"},
	{tbl = "RECOIL", key = "RECOVER_REF", label = "Recovery speed",
		step = 0.5, min = 2, max = 25, fmt = "%.1f",
		hint = "lower is slower and heavier"},
	{tbl = "RECOIL", key = "SIDE_IMPULSE", label = "Horizontal wander",
		step = 2, min = 0, max = 80, fmt = "%.0f",
		hint = "how much a burst walks sideways instead of straight up"},

	{group = "AIMING"},
	{tbl = "ADS", key = "RATE", label = "Sight-up speed",
		step = 0.2, min = 2, max = 12, fmt = "%.1f",
		hint = "lower is more deliberate"},
	{tbl = "CAM", key = "SENS_ADS_SCALE", label = "Sensitivity on sights",
		step = 0.05, min = 0.15, max = 1, fmt = "%.2f",
		hint = "optics slow your turn rate"},
	{tbl = "CAM", key = "SENS", label = "Mouse sensitivity",
		step = 0.0002, min = 0.0004, max = 0.006, fmt = "%.4f",
		hint = "radians per pixel"},
}

--------------------------------------------------------------------------------
-- PRESETS
--
-- ORIGINAL is exactly what the place shipped with, captured at load so it is
-- always recoverable. MILSIM leans on the three things that actually separate
-- ARMA and OPERATOR from an arcade shooter: the gun has mass and lags the
-- camera, it is never still, and the climb does not undo itself.
--------------------------------------------------------------------------------
local PRESETS = {
	{name = "ORIGINAL", values = {}},
	{name = "MILSIM", values = {
		INERTIA_ROT_ACCEL = 0.0225,
		INERTIA_MAX_ROT = 0.40,
		INERTIA_POS_ACCEL = 0.0115,
		INERTIA_ROT_D = 9.0,
		CAM_BREATH_AMPLITUDE_HIP = 0.031,
		CAM_BREATH_AMPLITUDE_ADS = 0.0165,
		INERTIA_IDLE_AMP = 0.025,
		RECOIL_RISE_IMPULSE = 62,
		RECOIL_PERMANENT = 0.34,
		RECOIL_RECOVER_REF = 8.5,
		ADS_RATE = 5.3,
		CAM_SENS_ADS_SCALE = 0.48,
	}},
	{name = "HEAVY", values = {
		INERTIA_ROT_ACCEL = 0.034,
		INERTIA_MAX_ROT = 0.52,
		INERTIA_POS_ACCEL = 0.016,
		INERTIA_ROT_D = 7.5,
		CAM_BREATH_AMPLITUDE_HIP = 0.042,
		CAM_BREATH_AMPLITUDE_ADS = 0.022,
		INERTIA_IDLE_AMP = 0.034,
		RECOIL_RISE_IMPULSE = 74,
		RECOIL_PERMANENT = 0.46,
		RECOIL_RECOVER_REF = 7.0,
		ADS_RATE = 4.6,
		CAM_SENS_ADS_SCALE = 0.42,
	}},
}

local function slot(k: any): string
	return k.tbl .. "_" .. k.key
end

-- Capture the shipped values so ORIGINAL is always a real destination.
for _, k in ipairs(KNOBS) do
	if k.tbl then
		PRESETS[1].values[slot(k)] = Cfg[k.tbl][k.key]
	end
end

local preset = 1

local function applyPreset(index: number)
	preset = ((index - 1) % #PRESETS) + 1
	local values = PRESETS[preset].values
	for _, k in ipairs(KNOBS) do
		if k.tbl then
			local v = values[slot(k)]
			if v == nil then v = PRESETS[1].values[slot(k)] end
			if v ~= nil then Cfg[k.tbl][k.key] = v end
		end
	end
end

--------------------------------------------------------------------------------
-- UI
--------------------------------------------------------------------------------
local gui = Instance.new("ScreenGui")
gui.Name = "BlacksiteFeelPanel"
gui.ResetOnSpawn = false
gui.IgnoreGuiInset = true
gui.DisplayOrder = 900
gui.Enabled = false
gui.Parent = player:WaitForChild("PlayerGui")

local root = Instance.new("Frame")
root.Size = UDim2.new(0, 470, 0, 620)
root.Position = UDim2.new(0, 18, 0.5, -310)
root.BackgroundColor3 = Color3.fromRGB(9, 11, 10)
root.BackgroundTransparency = 0.12
root.BorderSizePixel = 0
root.Parent = gui

local stroke = Instance.new("UIStroke")
stroke.Color = Color3.fromRGB(96, 108, 88)
stroke.Transparency = 0.5
stroke.Parent = root

local pad = Instance.new("UIPadding")
pad.PaddingTop = UDim.new(0, 12)
pad.PaddingLeft = UDim.new(0, 14)
pad.PaddingRight = UDim.new(0, 14)
pad.Parent = root

local layout = Instance.new("UIListLayout")
layout.SortOrder = Enum.SortOrder.LayoutOrder
layout.Padding = UDim.new(0, 2)
layout.Parent = root

local order = 0
local function line(size: number, colour: Color3, bold: boolean): TextLabel
	order += 1
	local t = Instance.new("TextLabel")
	t.LayoutOrder = order
	t.Size = UDim2.new(1, 0, 0, size + 6)
	t.BackgroundTransparency = 1
	t.Font = bold and Enum.Font.Code or Enum.Font.Code
	t.TextSize = size
	t.TextColor3 = colour
	t.TextXAlignment = Enum.TextXAlignment.Left
	t.RichText = true
	t.Text = ""
	t.Parent = root
	return t
end

local DIM = Color3.fromRGB(122, 132, 116)
local WARM = Color3.fromRGB(214, 206, 184)
local GOOD = Color3.fromRGB(138, 190, 122)
local BAD = Color3.fromRGB(214, 118, 96)

local title = line(16, WARM, true)
local bodyA = line(13, DIM)
local bodyB = line(13, DIM)
local bodyC = line(13, DIM)
line(6, DIM)
local presetLine = line(14, WARM)
line(6, DIM)

local rows = {}
for i, k in ipairs(KNOBS) do
	rows[i] = line(k.group and 13 or 14, k.group and DIM or WARM)
end
line(6, DIM)
local hint = line(12, DIM)
local keys = line(12, DIM)

title.Text = "BLACKSITE — FEEL          F4 to close"
keys.Text = "W/S or arrows select | A/D adjust | Q/E preset | R reset all"

--------------------------------------------------------------------------------
-- BODY DIAGNOSTICS
--
-- Answers the three questions that matter and cost four rounds to not be able
-- to answer: is the body where you are, can you see any of it, and did the
-- skeleton solve.
--------------------------------------------------------------------------------
local function bodyReport()
	local char = player.Character
	if not char then return "BODY   no character", DIM end

	local mode = char:GetAttribute("BlacksiteVisualBody")
	local visual = char:FindFirstChild("BlacksiteSoldierVisual")
	local head = char:FindFirstChild("Head")
	if not visual then
		return ("BODY   %s — no visual model"):format(tostring(mode)), BAD
	end

	local total, shown, sum = 0, 0, Vector3.zero
	for _, d in visual:GetDescendants() do
		if d:IsA("BasePart") then
			total += 1
			sum += d.Position
			if d.LocalTransparencyModifier < 0.95 and d.Transparency < 0.95 then
				shown += 1
			end
		end
	end
	if total == 0 then
		return ("BODY   %s — model is empty"):format(tostring(mode)), BAD
	end

	local at = sum / total
	local you = head and head.Position or (char:FindFirstChild("HumanoidRootPart")
		and char.HumanoidRootPart.Position) or at
	local away = (at - you).Magnitude

	local status, colour
	if away > 12 then
		status = ("%.0f studs AWAY from you — not attached"):format(away)
		colour = BAD
	elseif shown == 0 then
		status = "on you, but every piece is invisible"
		colour = BAD
	else
		status = ("on you, %d of %d pieces drawn"):format(shown, total)
		colour = GOOD
	end
	return ("BODY   %s — %s"):format(tostring(mode), status), colour
end

--------------------------------------------------------------------------------
-- INPUT
--------------------------------------------------------------------------------
local selected = 2 -- first non-group row

local function step(direction: number)
	local k = KNOBS[selected]
	if not k or not k.tbl then return end
	local v = (Cfg[k.tbl][k.key] or 0) + k.step * direction
	Cfg[k.tbl][k.key] = math.clamp(v, k.min, k.max)
end

local function move(direction: number)
	for _ = 1, #KNOBS do
		selected += direction
		if selected < 1 then selected = #KNOBS end
		if selected > #KNOBS then selected = 1 end
		if KNOBS[selected].tbl then return end
	end
end

UserInputService.InputBegan:Connect(function(input, processed)
	if processed then return end
	local k = input.KeyCode
	if k == Enum.KeyCode.F4 then
		gui.Enabled = not gui.Enabled
		return
	end
	if not gui.Enabled then return end

	if k == Enum.KeyCode.Down or k == Enum.KeyCode.S then
		move(1)
	elseif k == Enum.KeyCode.Up or k == Enum.KeyCode.W then
		move(-1)
	elseif k == Enum.KeyCode.Right or k == Enum.KeyCode.D then
		step(1)
	elseif k == Enum.KeyCode.Left or k == Enum.KeyCode.A then
		step(-1)
	elseif k == Enum.KeyCode.E then
		applyPreset(preset + 1)
	elseif k == Enum.KeyCode.Q then
		applyPreset(preset - 1)
	elseif k == Enum.KeyCode.R then
		applyPreset(1)
	end
end)

--------------------------------------------------------------------------------
-- DRAW
--------------------------------------------------------------------------------
local acc = 0
RunService.RenderStepped:Connect(function(dt)
	if not gui.Enabled then return end
	acc += dt
	if acc < 0.08 then return end
	acc = 0

	local text, colour = bodyReport()
	bodyA.Text = text
	bodyA.TextColor3 = colour

	local char = player.Character
	local hum = char and char:FindFirstChildOfClass("Humanoid")
	local hrp = char and char:FindFirstChild("HumanoidRootPart")
	if hum and hrp then
		local floor = hrp.Position.Y - (hum.HipHeight + hrp.Size.Y * 0.5)
		local head = char:FindFirstChild("Head")
		bodyB.Text = ("       rig %.2f studs tall, hips %.2f above the floor")
			:format(head and (head.Position.Y - floor) or 0, hrp.Position.Y - floor)
	else
		bodyB.Text = ""
	end
	bodyC.Text = "       F3 shows the effect pulses | F1 shows the controls"

	presetLine.Text = ("PRESET   &lt; <font color='#d6cbb8'>%s</font> &gt;   (Q/E)")
		:format(PRESETS[preset].name)

	for i, k in ipairs(KNOBS) do
		local row = rows[i]
		if k.group then
			row.Text = ("<font color='#6f7a66'>── %s ──</font>"):format(k.group)
		else
			local value = Cfg[k.tbl][k.key] or 0
			local base = PRESETS[1].values[slot(k)] or value
			local mark = (math.abs(value - base) > 1e-9) and "*" or " "
			local span = math.max(k.max - k.min, 1e-6)
			local filled = math.clamp(math.floor(((value - k.min) / span) * 14 + 0.5), 0, 14)
			local bar = string.rep("█", filled) .. string.rep("·", 14 - filled)
			local cursor = (i == selected) and "&gt;" or " "
			local tint = (i == selected) and "#d6cbb8" or "#8b9482"
			row.Text = ("<font color='%s'>%s %-26s %s %s%s</font>")
				:format(tint, cursor, k.label, bar, string.format(k.fmt, value), mark)
		end
	end

	local k = KNOBS[selected]
	hint.Text = k and k.hint and ("<i>" .. k.hint .. "</i>") or ""
end)

-- Ship on MILSIM rather than making the heavier feel something you have to go
-- and find. GameConfig keeps the v35 numbers so ORIGINAL is a real comparison
-- and one keypress away, which is the point: the difference between the two is
-- the thing that has been hard to describe in words.
applyPreset(2)

print("[BLACKSITE] FeelPanel ready -- press F4 to tune the feel, Q/E for presets")
