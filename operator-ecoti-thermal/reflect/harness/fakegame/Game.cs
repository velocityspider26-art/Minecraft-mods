// Mock of the OPERATOR types the mod probes, with the shapes the toolkit documents:
// statics on NvgLensFollower, the BrainAI.AllBrainAI registry, TeamIdentifier.TeamID,
// Mirror's NetworkServer.active/connections.
using System.Collections.Generic;
using UnityEngine;

namespace Il2CppMirror
{
    public class NetworkConnectionToClient { public int connectionId; }
    public static class NetworkServer
    {
        public static bool active { get; set; }
        public static bool activeHost { get; set; }
        public static Dictionary<int, NetworkConnectionToClient> connections { get; }
            = new Dictionary<int, NetworkConnectionToClient>();
    }
}

namespace Il2Cpp
{
    public static class NvgLensFollower
    {
        public static bool AnyActive { get; set; }
        public static int LensCount { get; set; }
        public static Color PhosphorColorValue { get; set; }
    }

    public class Health : Component { public bool isDead { get; set; } }

    public class TeamIdentifier : Component { public int TeamID { get; set; } }

    public class BrainAI : Component
    {
        public static List<BrainAI> AllBrainAI = new List<BrainAI>();
        public Health health { get; set; }
        public int TeamId { get; set; } = int.MinValue;
        public TeamIdentifier MyTeamIdentifierReference { get; set; }
    }

    public class PlayerMaster : Component { public static PlayerMaster MyPlayerMaster { get; set; } }

    public class MainCameraSingleton : Component
    {
        public static MainCameraSingleton instance { get; set; }
        public Camera mainCamera { get; set; }
    }

    public class ModInfo { public string ModName { get; set; } }
    public class CharacterMod : Component { public ModInfo ModInfo { get; set; } }
    public class CharacterModParent : Component { public CharacterMod currentMod { get; set; } }
    public class CharacterCustomisation : Component
    {
        public List<CharacterModParent> modParents { get; } = new List<CharacterModParent>();
    }
}
