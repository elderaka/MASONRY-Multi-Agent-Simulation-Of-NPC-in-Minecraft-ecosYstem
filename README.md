# MASONRY | Multi Agent Simulation Of NPC in Minecraft ecosYstem

A Minecraft Forge mod that implements a sophisticated NPC (Non-Player Character) system using Finite State Machines (FSM) for behavior control. This mod allows for the creation of intelligent agents that can interact with the Minecraft environment and other entities in a realistic way.

## Features

- Custom Entity Agent with FSM-based behavior control
- Multiple behavioral states:
  - IDLE: Basic observation and awareness
  - EXPLORE: Environmental exploration
  - COLLECT: Resource gathering (framework ready)
  - FIGHT: Combat behavior
- Modular and extensible architecture
- Easy-to-use spawn egg for testing

## Technical Details

- Built with Minecraft Forge
- Java-based implementation
- Uses Gradle for build management
- Implements a custom Finite State Machine for behavior control

## Development Setup

1. Clone the repository
2. Open the project in your preferred IDE (VSCode recommended)
3. Install required VSCode extensions:
   - Extension Pack for Java
   - Gradle for Java
   - Minecraft Development

## Building and Running

```bash
# Generate run configurations
./gradlew genEclipseRuns

# Run the client
./gradlew runClient
```

## Project Structure

```
src/main/java/com/example/examplemod/
├── entity/
│   ├── EntityAgent.java           # Main entity class
│   └── fsm/
│       ├── AgentState.java        # State definitions
│       └── AgentStateMachine.java # FSM implementation
├── init/
│   ├── ModEntities.java          # Entity registration
│   └── ModItems.java             # Item registration
└── ExampleMod.java               # Main mod class
```

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the All Rights Reserved License - see the LICENSE file for details.

## Acknowledgments

- Minecraft Forge team for the modding framework
- The Minecraft community for inspiration and support 