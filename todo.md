# TODO

- [ ] Improve pathfinding function
  - [ ] Prefer squares with high passability and in some cases diagonals.
  - [ ] Implement Dijkstra's, weight of "edge" is 1 / passability.
- [x] Create bookkeeping for commanding EC in bots and list of created bots in EC.
- [x] Let bots signal position of new EC's.
- [x] Let EC order attack on non-friendly EC.
- [x] Refactor RobotType-specific functions into separate class.
- [ ] Explore faster!
  - [ ] EC signal: "explore", all robots move away from each other
- [ ] Let Politicians wait with attack near EC until enough friendlies are near to convert the EC at once.
  - [ ] Check influence of nearby
  - [ ] 

- [ ] Let Muckrakers completely surround enemy EC or stay away.