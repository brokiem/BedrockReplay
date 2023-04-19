# BedrockReplay
A Minecraft: Bedrock Edition proxy replay system that allows players to record and replay their gameplay.
However, the project is still in development. If you find any bugs, please report them [here](https://github.com/brokiem/BedrockReplay/issues)

## Description
BedrockReplay is a replay system for Minecraft: Bedrock Edition that allows players to record and replay their gameplay. With Minecraft: Java Edition viewer support (SOON).

## Features
- Record and replay gameplay on Minecraft: Bedrock Edition
- Export replays to Minecraft: Java Edition viewer (SOON)
- Customizable replay settings, including playback speed and camera mode
- Share replays with friends and the Minecraft community

## Installation
1. Download Azul Zulu OpenJDK 11 for your operating system from [here](https://www.azul.com/downloads/zulu-community/?version=java-17-lts&package=jdk)
2. Download the artifact from the [actions page](https://github.com/brokiem/BedrockReplay/actions)
3. Extract the artifact to a directory
4. Double-click the BedrockReplay-SNAPSHOT.jar file to start the server

## Usage
1. Start BedrockReplay server by Double-clicking the BedrockReplay-SNAPSHOT.jar
2. Set the `address` and `port` in the `config.json` file located in the BedrockReplay directory
3. Join the proxy using Minecraft: Bedrock Edition
4. Type `!replay record <replayName>` in the chat to start recording
5. Type `!replay stop` in the chat to stop recording
6. Join the BedrockReplay viewer server using Minecraft: Bedrock Edition
7. Type `!replay play <replayName>` in the chat to start playing the replay
8. Check the available commands by typing `!` in the chat

## FAQ
**Q: Can I use BedrockReplay on Minecraft: Java Edition?**
A: No, BedrockReplay is designed specifically for Minecraft: Bedrock Edition. However, you can export replays to Minecraft: Java Edition viewer for viewing (SOON).

**Q: How do I customize the replay settings?**
A: Cooming soon

**Q: How do I record my singleplayer world?**
A: You can use Bedrock Dedicated Server to record your singleplayer world. Just set the `address` and `port` in the `config.json` file located in the BedrockReplay directory to the address and port of your Bedrock Dedicated Server.

**Q: Can I share my replays with friends and the Minecraft community?**
A: Yes, you can share your replay files with others. Just paste the replay file in the `replays` directory located in the BedrockReplay directory.

## License
BedrockReplay is released under the [MIT License](https://opensource.org/licenses/MIT).
