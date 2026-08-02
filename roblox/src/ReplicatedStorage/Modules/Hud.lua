--!nonstrict
-- Hud -- the whole player interface. Deliberately almost empty.
--
-- There is no ammo counter, no health bar, no crosshair and no hitmarker. You
-- learn your ammo state by checking the magazine, your health by how hard you
-- are breathing and how red the screen is, and your point of aim by using the
-- sights. Everything this module draws is either diegetic (something the
-- operator would actually perceive) or a control readout that fades away.
--
-- Required from both client scripts, which on the client share one instance.

local Players = game:GetService("Players")
local TweenService = game:GetService("TweenService")
local RunService = game:GetService("RunService")

local Cfg = require(script.Parent:WaitForChild("GameConfig"))

local Hud = {}

local MONO = Enum.Font.Code
local DIM = Color3.fromRGB(168, 172, 168)
local WARN = Color3.fromRGB(214, 132, 66)
local BAD = Color3.fromRGB(196, 62, 52)
local GOOD = Color3.fromRGB(126, 176, 122)

local gui: ScreenGui
local vignette: CanvasGroup
local damageFlash: Frame
local blackout: Frame
local subtitle: TextLabel
local statusLabel: TextLabel
local objectiveLabel: TextLabel
local promptLabel: TextLabel
local breathBar: Frame
local breathFill: Frame
local warnLabel: TextLabel
local helpFrame: Frame
local limbPanel: CanvasGroup
local limbParts: { [string]: Frame } = {}
local limbBleed: TextLabel
local limbHitLabel: TextLabel
local anatomyVitals: TextLabel
local anatomyInjury: TextLabel
local anatomyInternal: TextLabel
local limbValues: { [string]: number } = {}

local subtitleToken = 0
local statusToken = 0
local warnToken = 0

local function mk<T>(class: string, props: { [string]: any }, parent: Instance?): any
	local o = Instance.new(class)
	for k, v in props do
		(o :: any)[k] = v
	end
	if parent then
		o.Parent = parent
	end
	return o
end

local function edge(parent: Instance, size: UDim2, pos: UDim2, anchor: Vector2, rotation: number)
	local f = mk("Frame", {
		Size = size,
		Position = pos,
		AnchorPoint = anchor,
		BackgroundColor3 = Color3.new(0, 0, 0),
		BorderSizePixel = 0,
	}, parent)
	mk("UIGradient", {
		Rotation = rotation,
		Transparency = NumberSequence.new({
			NumberSequenceKeypoint.new(0, 0),
			NumberSequenceKeypoint.new(0.55, 0.75),
			NumberSequenceKeypoint.new(1, 1),
		}),
	}, f)
	return f
end


local function roundedPart(parent: Instance, name: string, size: UDim2, pos: UDim2, rotation: number): Frame
	local f = mk("Frame", {
		Name = name,
		Size = size,
		Position = pos,
		AnchorPoint = Vector2.new(0.5, 0.5),
		Rotation = rotation,
		BackgroundColor3 = GOOD,
		BackgroundTransparency = 0.42,
		BorderSizePixel = 0,
		ZIndex = 6,
	}, parent)
	mk("UICorner", { CornerRadius = UDim.new(0.35, 0) }, f)
	local stroke = mk("UIStroke", {
		Color = Color3.fromRGB(12, 14, 13),
		Transparency = 0.18,
		Thickness = 1,
	}, f)
	stroke.ApplyStrokeMode = Enum.ApplyStrokeMode.Border
	limbParts[name] = f
	return f
end

local function limbColor(frac: number): Color3
	frac = math.clamp(frac, 0, 1)
	if frac >= 0.62 then
		return GOOD:Lerp(DIM, (frac - 0.62) / 0.38)
	elseif frac >= 0.30 then
		return WARN:Lerp(GOOD, (frac - 0.30) / 0.32)
	end
	return BAD:Lerp(WARN, frac / 0.30)
end

local function buildLimbPanel(parent: Instance)
	limbPanel = mk("CanvasGroup", {
		Name = "LimbStatus",
		Size = UDim2.fromOffset(132, 220),
		Position = UDim2.fromOffset(22, 66),
		BackgroundColor3 = Color3.fromRGB(5, 7, 7),
		BackgroundTransparency = 0.56,
		GroupTransparency = 0.08,
		ZIndex = 5,
	}, parent)
	mk("UICorner", { CornerRadius = UDim.new(0, 5) }, limbPanel)
	mk("UIStroke", {
		Color = Color3.fromRGB(105, 112, 106),
		Transparency = 0.68,
		Thickness = 1,
	}, limbPanel)

	mk("TextLabel", {
		Name = "Title",
		Size = UDim2.new(1, -10, 0, 14),
		Position = UDim2.fromOffset(6, 3),
		BackgroundTransparency = 1,
		Font = MONO,
		Text = "TRAUMA",
		TextSize = 11,
		TextColor3 = DIM,
		TextTransparency = 0.25,
		TextXAlignment = Enum.TextXAlignment.Left,
		ZIndex = 7,
	}, limbPanel)

	roundedPart(limbPanel, "Head", UDim2.fromOffset(17, 17), UDim2.fromOffset(66, 27), 0)
	roundedPart(limbPanel, "Torso", UDim2.fromOffset(28, 40), UDim2.fromOffset(66, 57), 0)
	roundedPart(limbPanel, "LeftArm", UDim2.fromOffset(10, 43), UDim2.fromOffset(45, 60), 11)
	roundedPart(limbPanel, "RightArm", UDim2.fromOffset(10, 43), UDim2.fromOffset(87, 60), -11)
	roundedPart(limbPanel, "LeftLeg", UDim2.fromOffset(12, 49), UDim2.fromOffset(57, 108), 4)
	roundedPart(limbPanel, "RightLeg", UDim2.fromOffset(12, 49), UDim2.fromOffset(75, 108), -4)

	limbHitLabel = mk("TextLabel", {
		Name = "LastHit",
		Size = UDim2.new(1, -8, 0, 13),
		Position = UDim2.fromOffset(4, 17),
		BackgroundTransparency = 1,
		Font = MONO,
		Text = "",
		TextSize = 9,
		TextColor3 = WARN,
		TextTransparency = 1,
		TextXAlignment = Enum.TextXAlignment.Center,
		ZIndex = 8,
	}, limbPanel)

	anatomyVitals = mk("TextLabel", {
		Name = "Vitals",
		Size = UDim2.new(1, -10, 0, 14),
		Position = UDim2.fromOffset(5, 156),
		BackgroundTransparency = 1,
		Font = MONO,
		Text = "BLOOD 5.0 L   SHOCK 0%",
		TextSize = 9,
		TextColor3 = DIM,
		TextXAlignment = Enum.TextXAlignment.Left,
		ZIndex = 7,
	}, limbPanel)
	anatomyInjury = mk("TextLabel", {
		Name = "BoneInjury",
		Size = UDim2.new(1, -10, 0, 14),
		Position = UDim2.fromOffset(5, 172),
		BackgroundTransparency = 1,
		Font = MONO,
		Text = "",
		TextSize = 9,
		TextColor3 = WARN,
		TextXAlignment = Enum.TextXAlignment.Left,
		TextTruncate = Enum.TextTruncate.AtEnd,
		ZIndex = 7,
	}, limbPanel)
	anatomyInternal = mk("TextLabel", {
		Name = "InternalTrauma",
		Size = UDim2.new(1, -10, 0, 14),
		Position = UDim2.fromOffset(5, 188),
		BackgroundTransparency = 1,
		Font = MONO,
		Text = "",
		TextSize = 9,
		TextColor3 = BAD,
		TextXAlignment = Enum.TextXAlignment.Left,
		TextTruncate = Enum.TextTruncate.AtEnd,
		ZIndex = 7,
	}, limbPanel)
	limbBleed = mk("TextLabel", {
		Name = "Bleeding",
		Size = UDim2.new(1, -8, 0, 13),
		Position = UDim2.new(0, 4, 1, -15),
		BackgroundTransparency = 1,
		Font = MONO,
		Text = "EXTERNAL BLEED",
		TextSize = 10,
		TextColor3 = BAD,
		TextTransparency = 1,
		ZIndex = 7,
	}, limbPanel)
end

function Hud.build(): ScreenGui
	if gui and gui.Parent then
		return gui
	end
	local player = Players.LocalPlayer
	local pg = player:WaitForChild("PlayerGui")

	gui = mk("ScreenGui", {
		Name = "BlacksiteUI",
		ResetOnSpawn = false,
		IgnoreGuiInset = true,
		DisplayOrder = 10,
		ZIndexBehavior = Enum.ZIndexBehavior.Sibling,
	}, pg)

	-- Vignette --------------------------------------------------------------
	vignette = mk("CanvasGroup", {
		Name = "Vignette",
		Size = UDim2.fromScale(1, 1),
		BackgroundTransparency = 1,
		GroupTransparency = 0.62,
		ZIndex = 2,
	}, gui)
	edge(vignette, UDim2.fromScale(1, 0.30), UDim2.fromScale(0, 0), Vector2.new(0, 0), 90)
	edge(vignette, UDim2.fromScale(1, 0.32), UDim2.fromScale(0, 1), Vector2.new(0, 1), 270)
	edge(vignette, UDim2.fromScale(0.26, 1), UDim2.fromScale(0, 0), Vector2.new(0, 0), 0)
	edge(vignette, UDim2.fromScale(0.26, 1), UDim2.fromScale(1, 0), Vector2.new(1, 0), 180)

	-- Damage flash ----------------------------------------------------------
	damageFlash = mk("Frame", {
		Name = "Damage",
		Size = UDim2.fromScale(1, 1),
		BackgroundColor3 = Color3.fromRGB(90, 8, 8),
		BackgroundTransparency = 1,
		BorderSizePixel = 0,
		ZIndex = 3,
	}, gui)

	blackout = mk("Frame", {
		Name = "Blackout",
		Size = UDim2.fromScale(1, 1),
		BackgroundColor3 = Color3.new(0, 0, 0),
		BackgroundTransparency = 1,
		BorderSizePixel = 0,
		ZIndex = 8,
	}, gui)

	-- Text ------------------------------------------------------------------
	objectiveLabel = mk("TextLabel", {
		Name = "Objective",
		Size = UDim2.new(0, 460, 0, 20),
		Position = UDim2.new(0, 26, 0, 22),
		BackgroundTransparency = 1,
		Font = MONO,
		TextSize = 15,
		TextColor3 = DIM,
		TextXAlignment = Enum.TextXAlignment.Left,
		TextTransparency = 0.25,
		Text = "",
		ZIndex = 5,
	}, gui)

	statusLabel = mk("TextLabel", {
		Name = "Status",
		Size = UDim2.new(0, 460, 0, 20),
		Position = UDim2.new(0, 26, 1, -34),
		BackgroundTransparency = 1,
		Font = MONO,
		TextSize = 15,
		TextColor3 = DIM,
		TextXAlignment = Enum.TextXAlignment.Left,
		TextTransparency = 1,
		Text = "",
		ZIndex = 5,
	}, gui)

	subtitle = mk("TextLabel", {
		Name = "Subtitle",
		Size = UDim2.new(1, -80, 0, 24),
		Position = UDim2.new(0.5, 0, 0.74, 0),
		AnchorPoint = Vector2.new(0.5, 0.5),
		BackgroundTransparency = 1,
		Font = MONO,
		TextSize = 17,
		TextColor3 = Color3.fromRGB(220, 222, 218),
		TextTransparency = 1,
		Text = "",
		ZIndex = 5,
	}, gui)

	promptLabel = mk("TextLabel", {
		Name = "Prompt",
		Size = UDim2.new(1, -80, 0, 22),
		Position = UDim2.new(0.5, 0, 0.60, 0),
		AnchorPoint = Vector2.new(0.5, 0.5),
		BackgroundTransparency = 1,
		Font = MONO,
		TextSize = 16,
		TextColor3 = Color3.fromRGB(232, 226, 200),
		TextTransparency = 1,
		Text = "",
		ZIndex = 5,
	}, gui)

	warnLabel = mk("TextLabel", {
		Name = "Warn",
		Size = UDim2.new(0, 420, 0, 20),
		Position = UDim2.new(0.5, 0, 0.68, 0),
		AnchorPoint = Vector2.new(0.5, 0.5),
		BackgroundTransparency = 1,
		Font = MONO,
		TextSize = 15,
		TextColor3 = WARN,
		TextTransparency = 1,
		Text = "",
		ZIndex = 5,
	}, gui)

	-- Breath / stamina. Only shows when it matters.
	breathBar = mk("Frame", {
		Name = "Breath",
		Size = UDim2.new(0, 150, 0, 2),
		Position = UDim2.new(0.5, 0, 1, -40),
		AnchorPoint = Vector2.new(0.5, 0.5),
		BackgroundColor3 = Color3.fromRGB(28, 30, 28),
		BackgroundTransparency = 1,
		BorderSizePixel = 0,
		ZIndex = 5,
	}, gui)
	breathFill = mk("Frame", {
		Size = UDim2.fromScale(1, 1),
		BackgroundColor3 = DIM,
		BackgroundTransparency = 1,
		BorderSizePixel = 0,
		ZIndex = 6,
	}, breathBar)

	buildLimbPanel(gui)
	Hud.buildHelp()
	return gui
end

--------------------------------------------------------------------------------
-- Public surface
--------------------------------------------------------------------------------

-- One line of the operator's own read on the situation. Used for mag checks,
-- chamber checks, medical calls -- the things a HUD would normally tell you.
function Hud.say(text: string, duration: number?, color: Color3?)
	if not subtitle then
		return
	end
	subtitleToken += 1
	local token = subtitleToken
	subtitle.Text = text
	subtitle.TextColor3 = color or Color3.fromRGB(220, 222, 218)
	subtitle.TextTransparency = 0
	task.delay(duration or 2.6, function()
		if subtitleToken ~= token then
			return
		end
		TweenService:Create(subtitle, TweenInfo.new(0.7), { TextTransparency = 1 }):Play()
	end)
end

-- Control readout: stance, fire selector, torch. Fades out on its own.
function Hud.status(text: string)
	if not statusLabel then
		return
	end
	statusToken += 1
	local token = statusToken
	statusLabel.Text = text
	statusLabel.TextTransparency = 0.15
	task.delay(2.2, function()
		if statusToken ~= token then
			return
		end
		TweenService:Create(statusLabel, TweenInfo.new(0.9), { TextTransparency = 1 }):Play()
	end)
end

function Hud.warn(text: string, duration: number?)
	if not warnLabel then
		return
	end
	warnToken += 1
	local token = warnToken
	warnLabel.Text = text
	warnLabel.TextTransparency = 0
	task.delay(duration or 3.0, function()
		if warnToken ~= token then
			return
		end
		TweenService:Create(warnLabel, TweenInfo.new(0.6), { TextTransparency = 1 }):Play()
	end)
end

function Hud.objective(text: string)
	if objectiveLabel then
		objectiveLabel.Text = text
	end
end

function Hud.prompt(text: string?)
	if not promptLabel then
		return
	end
	if text and text ~= "" then
		promptLabel.Text = text
		promptLabel.TextTransparency = 0.1
	else
		promptLabel.TextTransparency = 1
	end
end

-- 0 = calm, 1 = about to be killed.
function Hud.setDread(v: number)
	if vignette then
		vignette.GroupTransparency = 0.62 - 0.5 * math.clamp(v, 0, 1)
	end
end

function Hud.setHealth(frac: number)
	if not damageFlash then
		return
	end
	-- Persistent red haze that deepens as you bleed out.
	local hurt = 1 - math.clamp(frac, 0, 1)
	damageFlash.BackgroundTransparency = 1 - hurt * 0.42
end

function Hud.hitFlash()
	if not damageFlash then
		return
	end
	damageFlash.BackgroundTransparency = 0.28
end

function Hud.setBreath(frac: number, holding: boolean)
	if not breathBar then
		return
	end
	local show = frac < 0.995 or holding
	local alpha = show and 0.35 or 1
	breathBar.BackgroundTransparency = math.min(alpha + 0.3, 1)
	breathFill.BackgroundTransparency = alpha
	breathFill.Size = UDim2.fromScale(math.clamp(frac, 0, 1), 1)
	breathFill.BackgroundColor3 = frac < 0.25 and BAD or (frac < 0.55 and WARN or DIM)
end


function Hud.setLimbStatus(values: { [string]: number }, bleeding: boolean)
	if not limbPanel then
		return
	end
	local worst = 1
	for group, frame in limbParts do
		local frac = math.clamp(values[group] or 1, 0, 1)
		limbValues[group] = frac
		worst = math.min(worst, frac)
		frame.BackgroundColor3 = limbColor(frac)
		frame.BackgroundTransparency = 0.24 + frac * 0.30
	end
	limbPanel.GroupTransparency = (worst < 0.98 or bleeding) and 0 or 0.28
	if limbBleed then
		limbBleed.TextTransparency = bleeding and 0.05 or 1
	end
end

function Hud.setAnatomyStatus(data: { [string]: any })
	if not limbPanel then return end
	local bloodMl = typeof(data.bloodMl) == "number" and data.bloodMl or 5000
	local shock = typeof(data.shock) == "number" and math.clamp(data.shock, 0, 1) or 0
	local pain = typeof(data.pain) == "number" and math.clamp(data.pain, 0, 100) or 0
	local external = typeof(data.externalBleed) == "number" and data.externalBleed or 0
	local internal = typeof(data.internalBleed) == "number" and data.internalBleed or 0
	local bone = typeof(data.lastBone) == "string" and data.lastBone or ""
	local organ = typeof(data.lastOrgan) == "string" and data.lastOrgan or ""
	local fracture = typeof(data.fracture) == "number" and data.fracture or 0

	if anatomyVitals then
		anatomyVitals.Text = ("BLOOD %.1f L   SHOCK %d%%"):format(bloodMl / 1000, math.floor(shock * 100 + .5))
		local bloodFrac = math.clamp(bloodMl / 5000, 0, 1)
		anatomyVitals.TextColor3 = bloodFrac < .48 and BAD or (bloodFrac < .72 and WARN or DIM)
	end
	if anatomyInjury then
		if fracture > 0 and bone ~= "" then
			local severity = ({"CRACK", "FRACTURE", "SHATTERED"})[fracture] or "BONE TRAUMA"
			anatomyInjury.Text = severity .. ": " .. string.upper(bone)
		else
			anatomyInjury.Text = pain >= 30 and ("PAIN " .. tostring(math.floor(pain + .5)) .. "%") or ""
		end
	end
	if anatomyInternal then
		if internal > .35 then
			anatomyInternal.Text = organ ~= "" and ("INTERNAL: " .. string.upper(organ)) or "INTERNAL BLEEDING"
		elseif organ ~= "" and pain >= 45 then
			anatomyInternal.Text = "ORGAN TRAUMA: " .. string.upper(organ)
		else
			anatomyInternal.Text = ""
		end
	end
	if limbBleed then
		limbBleed.TextTransparency = external > .15 and 0.05 or 1
	end
	if bloodMl < 4925 or shock > .03 or external > .15 or internal > .15 or fracture > 0 then
		limbPanel.GroupTransparency = 0
	end
end

function Hud.hitLimb(group: string?)
	local frame = group and limbParts[group]
	if not frame then
		return
	end
	local labels = {
		Head = "HEAD HIT", Torso = "TORSO HIT",
		LeftArm = "LEFT ARM", RightArm = "RIGHT ARM",
		LeftLeg = "LEFT LEG", RightLeg = "RIGHT LEG",
	}
	if limbHitLabel then
		limbHitLabel.Text = labels[group :: string] or string.upper(group :: string)
		limbHitLabel.TextTransparency = 0
		local captured = limbHitLabel.Text
		task.delay(1.15, function()
			if limbHitLabel and limbHitLabel.Parent and limbHitLabel.Text == captured then
				TweenService:Create(limbHitLabel, TweenInfo.new(0.35), { TextTransparency = 1 }):Play()
			end
		end)
	end
	frame.BackgroundColor3 = Color3.new(1, 1, 1)
	frame.BackgroundTransparency = 0.02
	task.delay(0.13, function()
		if frame.Parent then
			local frac = limbValues[group :: string] or 1
			TweenService:Create(frame, TweenInfo.new(0.26), {
				BackgroundColor3 = limbColor(frac),
				BackgroundTransparency = 0.24 + frac * 0.30,
			}):Play()
		end
	end)
end

function Hud.fade(to: number, time: number)
	if blackout then
		TweenService:Create(blackout, TweenInfo.new(time), { BackgroundTransparency = 1 - to }):Play()
	end
end

--------------------------------------------------------------------------------
-- Controls card (F1)
--------------------------------------------------------------------------------
local HELP = {
	{ "W A S D", "move" },
	{ "SHIFT", "sprint (weapon comes down)" },
	{ "CTRL", "slow walk -- quiet" },
	{ "C", "tap: crouch / HOLD + mouse: exact height" },
	{ "Q / E", "hold to lean, release to come up" },
	{ "G", "low ready -- rests your arms" },
	{ "ALT", "free look" },
	{ "", "" },
	{ "MOUSE 1", "fire" },
	{ "MOUSE 2", "aim down sights" },
	{ "SHIFT (aiming)", "hold breath" },
	{ "R", "tap: tactical reload / hold: dump the mag" },
	{ "T", "check magazine" },
	{ "Y", "check chamber" },
	{ "X", "fire selector -- starts on SEMI, SAFE is one click back" },
	{ "V", "inspect weapon" },
	{ "B", "suppressor" },
	{ "1 / 2", "primary / sidearm" },
	{ "", "" },
	{ "F", "interact" },
	{ "L", "weapon light" },
	{ "K", "laser" },
	{ "N", "night vision" },
	{ "H", "pressure dressing" },
	{ "F1", "close" },
}

function Hud.buildHelp()
	helpFrame = mk("Frame", {
		Name = "Help",
		Size = UDim2.new(0, 470, 0, 26 + #HELP * 20),
		Position = UDim2.fromScale(0.5, 0.5),
		AnchorPoint = Vector2.new(0.5, 0.5),
		BackgroundColor3 = Color3.fromRGB(8, 9, 10),
		BackgroundTransparency = 0.12,
		BorderSizePixel = 0,
		Visible = false,
		ZIndex = 20,
	}, gui)
	mk("UIStroke", { Color = Color3.fromRGB(50, 54, 50), Thickness = 1, Transparency = 0.4 }, helpFrame)

	for i, row in HELP do
		mk("TextLabel", {
			Size = UDim2.new(0, 150, 0, 18),
			Position = UDim2.new(0, 18, 0, 14 + (i - 1) * 20),
			BackgroundTransparency = 1,
			Font = MONO,
			TextSize = 14,
			TextColor3 = GOOD,
			TextXAlignment = Enum.TextXAlignment.Left,
			Text = row[1],
			ZIndex = 21,
		}, helpFrame)
		mk("TextLabel", {
			Size = UDim2.new(0, 290, 0, 18),
			Position = UDim2.new(0, 172, 0, 14 + (i - 1) * 20),
			BackgroundTransparency = 1,
			Font = MONO,
			TextSize = 14,
			TextColor3 = DIM,
			TextXAlignment = Enum.TextXAlignment.Left,
			Text = row[2],
			ZIndex = 21,
		}, helpFrame)
	end
end

function Hud.toggleHelp(): boolean
	if not helpFrame then
		return false
	end
	helpFrame.Visible = not helpFrame.Visible
	return helpFrame.Visible
end

Hud.COLORS = { DIM = DIM, WARN = WARN, BAD = BAD, GOOD = GOOD }
Hud.Cfg = Cfg
Hud.RunService = RunService

return Hud
