# Battlecode Strategy Document

Here we write down some considerations and plans for writing the four robots.

## Robots in short

Also explained quite well [here](https://2021.battlecode.org/specs/specs.md.html).

- Enlightenment center (EC): base to create robots
- Politician: suicide units, can use "empower" once to transfer their conviction to nearby units. Seem only useful to convert buildings at first, since no net conviction is gained, but some interactions change this. Only empower if:
  - Targeting a building
  - Targeting enemy muckrakers to protect own slanderers
  - Speech bonus is enhanced by successful muckraking.
- Slanderer:
  - mobile conviction generator (both for itself and its EC of origin). Generation rate [depends on current conviction](https://www.wolframalpha.com/input/?i=plot+%281%2F50+%2B+0.03+*+e%5E%28-0.001*x%29%29+*+x+from+x%3D0+to+1000).
  - turns into politician after 300 rounds.
- Muckraker

## Win strategies

- Aggressive: focus on destroying all enemy robots; collect more influence and send politicians to convert everyone before round 3000.
- Defensive: focus on votes; win after 3000 rounds.

## Global subgoals

- Protect slanderers from muckrakers by speeching muckrakers with politicians
- Muckrake enemy slanderers to boost speech power
- Collect influence by capturing EC's and building slanderers
- Protect bases from conversion by enemy politicians

## Unit composition

- If enemy has many slanderers, build muckrakers
- If enemy has many muckrakers, build politicians
- Otherwise, build slanderers

## Overall strategy idea

1. Build a muckraker to scout, return with intel on robot types (muckraker sets flag)
2. Build more robots depending on intel
3. Keep slanderers close together in a corner, with politicians around them to protect from muckrakers.

## Individual robot strategy ideas

### Enlightenment Center (EC)

Start in economic mode, with first one muckraker to find the enemy base and destroy slanderers if possible.

- **Economic mode**: create Muck, Poli and Slan in 1:2:3 proportions to gain a little intel, have solid protection for slanderers (politicians also self-destruct) and generate lots of influence.
- **Offensive mode**: create mainly muckrakers and some politicians to destroy all enemy slanderers and other robots.
- **Defensive mode**: create mainly politicians to defend slanderers from enemy muckrakers and base from politicians.

### Muckraker

- Move around the map hunting for slanderers
- Stay away from enemy politicians
- Report back unknown (enemy) base locations

### Slanderer

- Move away from enemy base if location is known
- Move towards friendly slanderers (clump up)
- Stay close to the base but keep space for new spawns.

### Politician

- **Defensive**: Move towards enemy base, but stay in range of a friendly slanderer or EC.
- **Offensive**: Move towards enemy base and try to convert it.
- When spotting an enemy muckraker:
  1. Update flag
  2. Move towards the muckraker
  3. Speech

## Pathfinding

Every Robot moves by setting a destination and then calculating a Direction based on it. As robots move they store information about their surroundings in a map, and assuming some passability (1?) of unseen tiles they calculate a shortest path with the A*-algorithm.

##### Path = 1 direction + N direction changes

Once a robot finds a shortest path between two EC's, it reports it as a sequence of directions, encoded in multiple messages over approx. 10 turns (see "Flag communication" message M2 and M3). We start by transmitting an initial direction, followed by a sequence of direction changes. Each **direction change can take 5 possible values**: turn left, slight left, slight right, turn right and straight). We can encode this in a quintal system, with "5-bits" that can take five values, so that each 5-bit encodes one move. In total a path of N steps is thus stored by a number between 0 and 5^N.

Unfortunately, in only message M2 we could only store a measly 7 moves. Therefore, we transmit the large number required to represent the path in M2 and subsequent M3 messages. For a path of 64 moves, we need 149 bits, which boils down to 2*M2 + ceil(149/21) *M3 = 10 messages.

## Flag communications

Each robot has a 24-bit flag. Robots can see each others' flags if they are in sensor range, and EC's can query the flag of a robot whose ID they have and vice versa. We use 14 bits to transmit a location, and 10 bits for any other information.

### Communication direction 1: Mobile robot to (master) EC

We use the bits as follows for bots communicating to the EC, we call them M(obile)-messages:

```
0  - Bits 0-2 = Message type. No message = 0, Report EC location = 1,
1    Report shortest path = 2 & 3
2
```

The different messages use the rest of the bits differently.

#### Message M0: Nothing to report

#### Message M1: Report EC location

```
3 
4
5
6  - Bits 6-7: conviction of target EC
7    4 possible values; 0, 80, 200, 500 or more
8  - Team Neutral = 1
9  - Team A = 0, Team B = 1

// Location follows (for EC location report)

10 - Bits up to 23: 14 bits of location information
11   encoded by 128 * (x % 128) + (y % 128)   
...
23
```

#### Messages M2 and M3: reporting shortest paths.

See "Pathfinding", we need about 10 turns to submit a complete shortest path using two message types. The first two messages explicitly define the locations of the ECs to connect, so that any robot can transmit a shortest path from anywhere, and each EC that has seen the robot before will receive it. (We don't optimize by making the master EC the implicit start of the path and communicating in sensor range of the end-of-path EC).

##### Message M2: Report shortest path - start of communication

A path-report is started with two messages M2.

```
3 - Bits 3-6: Initial direction
...
7 - First or second EC: 0 = first, 1 = second.
..
10 - Bits 10 - 23 encode the location of the EC that the bot is talking about.

```

##### Message M3: Report shortest path - rest of communication

```
3 - 21 Bits 3-23 encode the rest of the path. The end-of-path is transmitted by two subsequent turns to the right (always suboptimal).
```

### Communication direction 1: EC to mobile robots

```
0  - Bits 0-2 = Message type. No message = 0, Attack = 1,
1    Report shortest path = 2 & 3
2
...
```

#### Message E0: No message

#### Message E1: Attack Location

```
9  - Bot type: 0 = Politician (Attack EC), 1 = Muckraker (Destroy slanderers).
// Location follows
10 - Bits up to 23: 14 bits of location information
11   encoded by 128 * (x % 128) + (y % 128)   
...
23
```

#### Messages E2 and E3: reporting shortest paths.

See M2 and M3.