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

## Flag communications

Each robot has a 24-bit flag. Robots can see each others' flags if they are in sensor range, and EC's can query the flag of a robot whose ID they have and vice versa. We use 14 bits to transmit a location, and 10 bits for any other information.

We use the bits as follows for bots communicating to the EC:

```
// "Extra information" (non-location)

0  - Signaling=1, Not signaling=0  (rest of bits should also be 0)
1
2
3
4
5
6  - Bits 6-7: conviction of target EC
7    4 possible values; 0, 80, 200, 500 or more
8  - Team Neutral = 1
9  - Team A = 0, Team B = 1

// Location follows

10 - Bits up to 23: 14 bits of location information
11   encoded by 128 * (x % 128) + (y % 128)   
...
23
```

And the following flags to communicate from the EC to moving bots:

```
// "Extra information" (non-location)

0  - Attack=1, Explore=0
1  - ...
9

// Location follows

10 - Bits up to 23: 14 bits of location information
11   encoded by 128 * (x % 128) + (y % 128)   
...
23
```