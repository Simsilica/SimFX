# SimFX

General video game effects packaged in jMonkeyEngine friendly components.

Currently includes:

## LightingState

A jMonkeyEngine AppState that manages a DirectionalLight and AmbientLight making the parameters easily available to the rest of the game.

## SkyState

Includes related shaders and a TruncatedDome mesh.  This is an AppState that allows a game to easily include a sky based
on atmospheric scattering.  An optional ground plane can be included and there are lighting shaders that include 
atmospheric scattering that your own terrain and models can use.  The atmospheric scattering code is also separated into 
shader libraries that can be included in other shaders.
