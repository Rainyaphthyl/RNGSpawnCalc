# RNG calculator for optimal witch farms

## Purpose
The program takes the following input
- The position of the player AFK spot, and the chunk pos and height span of the witch huts
  (A witch hut always spans chunk coordinates (0, 0) to (8, 6))
- The heights for the floors of the spawn chamber
- World seed and world spawn location

and gives the following output: 
- The woodland mansion region for the optimal witch farm

## Setup
The perimeter must include up to 4 witch huts, and all chunks eligible for
 spawning must have bedrock removed to y=0, y=1~13 filled with sand, then y=14 filled with jack-o-lanterns, and
 then slime blocks at y=15. 
This is to make any possibility of a pack spawn attempt in this chunk choose a normal cube block, and thus fail. 
Then the spawning for this chunk will complete after precisely three random number generator calls. 
The four chunks containing witch huts must be filled with bedrock, sand and jack-o-lanterns up to y=79. If
 spawn-proofing at those heights need to be achieved without using light sources, bedrock should be used to cover
 surfaces. y=79 should be covered with slime blocks or barrier to avoid incrementing heightmaps and block spawns. 
The heights of the witch hut must be free of normal cubes, so that pack spawn attempts for witch huts can occur, and
 also this program can compute things more easily. Redstone blocks or observers should be used as surfaces for
 witches to spawn on. Ghast sweepers or frameless light suppressed nether portals should be used to remove the
 witches from the spawn chamber. 

## Problem and algorithm
Under the previously mentioned setup, the spawning algorithm is reduced to three random calls for a non-hut chunk,
 and for a hut chunk, a pack spawn start is initially chosen. If it is blocked, then this chunk also completes
 after three random calls. If it is at the height of the witch hut, the following sequence of operations are attempted
 three times: 
- Invoke Math.random() to choose a value between 1 and 4 as the number of pack spawns
- For each pack spawn, call the world random six times to determine the location of the spawn,
   and one more time to determine the type of the mob. 
- If 4 successful spawns has occurred, the spawning of this chunk terminates. 

This causes the number of random call advancements to be taken from 3+7n, where n is either 0 or between 3 and 12. 
The randomness should be relatively controllable. Then the expected number of witch spawns in this chunk can be
 determined, and also the possible initial states of the next chunk and the respective chances. 

After a fixed number of dummy chunks, we arrive at the next witch hut chunk. This time we have up to 11 possible
 initial values, and for each initial value, we compute the expected number of witch spawns, and the possible
 initial states of the next chunk with chances. Then after the second chunk, we have the expected number of spawns
 of that chunk, and up to 23 possible initial values with chances. 

We repeat this process for the third witch hut chunk and arrive at the expected number of spawns of that chunk, and
 up to 35 possible initial values of the fourth chunk. For the fourth chunk we just compute the expected spawns. 

After this procedure, we should sum the four numbers of expected spawns to get the total number of expected spawns
 per game tick. We want to do a brute force search through all possible initial states achievable through world
 RNG manipulation. 