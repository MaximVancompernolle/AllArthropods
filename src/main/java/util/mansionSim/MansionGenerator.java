package util.mansionSim;

import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.util.block.BlockDirection;
import com.seedfinding.mccore.util.block.BlockMirror;
import com.seedfinding.mccore.util.block.BlockRotation;
import com.seedfinding.mccore.util.data.Pair;
import com.seedfinding.mccore.util.pos.BPos;
import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcfeature.structure.generator.Generator;
import com.seedfinding.mcterrain.TerrainGenerator;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MansionGenerator extends Generator {

    private final List<MansionPiece> globalPieces;

    public MansionGenerator(MCVersion version) {
        super(version);
        globalPieces = new ArrayList<>();

    }

    public void reset() {
        this.globalPieces.clear();
    }

    public List<MansionPiece> getPieces() {
        return globalPieces;
    }

    @Override
    public boolean generate(TerrainGenerator generator, int chunkX, int chunkZ, ChunkRand rand) {
        if (generator == null) return false;
        int y = util.mansionSim.Mansion.getAverageYPosition(generator, chunkX, chunkZ);
        if (y < 60) return false;
        rand.setCarverSeed(generator.getWorldSeed(), chunkX, chunkZ, this.getVersion());
        BlockRotation rotation = BlockRotation.getRandom(rand);
        BPos start = new BPos(chunkX * 16 + 8, y + 1, chunkZ * 16 + 8);
        return this.start(start, rotation, this.globalPieces, rand);
    }

    // skips some checks and inaccurate height but faster
    public boolean fastGenerate(long worldSeed, int chunkX, int chunkZ, ChunkRand rand) {
        rand.setCarverSeed(worldSeed, chunkX, chunkZ, this.getVersion());
        BlockRotation rotation = BlockRotation.getRandom(rand);
        BPos start = new BPos(chunkX * 16 + 8, 64 + 1, chunkZ * 16 + 8);
        return this.start(start, rotation, this.globalPieces, rand);
    }

    public boolean start(BPos start, BlockRotation rotation, List<MansionPiece> mansionPieces, ChunkRand chunkRand) {
        Grid grid = new Grid(chunkRand);
        Placer placer = new Placer(chunkRand);
        placer.createMansion(start, rotation, mansionPieces, grid);
        return true;
    }

    @Override
    public List<Pair<ILootType, BPos>> getChestsPos() {
        return null;
    }

    @Override
    public List<Pair<ILootType, BPos>> getLootPos() {
        return null;
    }

    @Override
    public ILootType[] getLootTypes() {
        return new ILootType[0];
    }

    static class SimpleGrid {
        private final int[][] grid;
        private final int width;
        private final int height;
        private final int valueIfOutside;

        public SimpleGrid(int width, int height, int valueIfOutside) {
            this.width = width;
            this.height = height;
            this.valueIfOutside = valueIfOutside;
            this.grid = new int[width][height];
        }

        public void set(int x, int z, int value) {
            if (x >= 0 && x < this.width && z >= 0 && z < this.height) {
                this.grid[x][z] = value;
            }

        }

        public void set(int minX, int minZ, int maxX, int maxZ, int value) {
            for (int i = minZ; i <= maxZ; ++i) {
                for (int j = minX; j <= maxX; ++j) {
                    this.set(j, i, value);
                }
            }

        }

        public int get(int x, int z) {
            return x >= 0 && x < this.width && z >= 0 && z < this.height ? this.grid[x][z] : this.valueIfOutside;
        }

        public void setif(int x, int z, int conditionValue, int newValue) {
            if (this.get(x, z) == conditionValue) {
                this.set(x, z, newValue);
            }

        }

        public boolean adjacentTo(int x, int z, int value) {
            return this.get(x - 1, z) == value || this.get(x + 1, z) == value || this.get(x, z + 1) == value || this.get(x, z - 1) == value;
        }
    }

    static class Grid {
        private final ChunkRand random;
        private final SimpleGrid baseGrid;
        private final SimpleGrid thirdFloorGrid;
        private final SimpleGrid[] floorGrids;
        private final int entranceX;
        private final int entranceZ;

        public Grid(ChunkRand random) {
            this.random = random;
            this.entranceX = 7;
            this.entranceZ = 4;
            this.baseGrid = new SimpleGrid(11, 11, BaseRoomFlag.OUTSIDE);
            this.baseGrid.set(this.entranceX, this.entranceZ, this.entranceX + 1, this.entranceZ + 1, BaseRoomFlag.START);
            this.baseGrid.set(this.entranceX - 1, this.entranceZ, this.entranceX - 1, this.entranceZ + 1, BaseRoomFlag.ROOM);
            this.baseGrid.set(this.entranceX + 2, this.entranceZ - 2, this.entranceX + 3, this.entranceZ + 3, BaseRoomFlag.OUTSIDE);
            this.baseGrid.set(this.entranceX + 1, this.entranceZ - 2, this.entranceX + 1, this.entranceZ - 1, BaseRoomFlag.CORRIDOR);
            this.baseGrid.set(this.entranceX + 1, this.entranceZ + 2, this.entranceX + 1, this.entranceZ + 3, BaseRoomFlag.CORRIDOR);
            this.baseGrid.set(this.entranceX - 1, this.entranceZ - 1, BaseRoomFlag.CORRIDOR);
            this.baseGrid.set(this.entranceX - 1, this.entranceZ + 2, BaseRoomFlag.CORRIDOR);
            this.baseGrid.set(0, 0, 11, 1, BaseRoomFlag.OUTSIDE);
            this.baseGrid.set(0, 9, 11, 11, BaseRoomFlag.OUTSIDE);
            this.recursiveCorridor(this.baseGrid, this.entranceX, this.entranceZ - 2, BlockDirection.WEST, 6);
            this.recursiveCorridor(this.baseGrid, this.entranceX, this.entranceZ + 3, BlockDirection.WEST, 6);
            this.recursiveCorridor(this.baseGrid, this.entranceX - 2, this.entranceZ - 1, BlockDirection.WEST, BaseRoomFlag.START);
            this.recursiveCorridor(this.baseGrid, this.entranceX - 2, this.entranceZ + 2, BlockDirection.WEST, BaseRoomFlag.START);

            while (this.cleanEdges(this.baseGrid)) {
            }

            this.floorGrids = new SimpleGrid[3];
            this.floorGrids[0] = new SimpleGrid(11, 11, BaseRoomFlag.OUTSIDE);
            this.floorGrids[1] = new SimpleGrid(11, 11, BaseRoomFlag.OUTSIDE);
            this.floorGrids[2] = new SimpleGrid(11, 11, BaseRoomFlag.OUTSIDE);
            this.identifyRooms(this.baseGrid, this.floorGrids[0]);
            this.identifyRooms(this.baseGrid, this.floorGrids[1]);
            this.floorGrids[0].set(this.entranceX + 1, this.entranceZ, this.entranceX + 1, this.entranceZ + 1, RoomGroupFlag.ENTRANCE);
            this.floorGrids[1].set(this.entranceX + 1, this.entranceZ, this.entranceX + 1, this.entranceZ + 1, RoomGroupFlag.ENTRANCE);
            this.thirdFloorGrid = new SimpleGrid(this.baseGrid.width, this.baseGrid.height, BaseRoomFlag.OUTSIDE);
            this.setupThirdFloor();
            this.identifyRooms(this.thirdFloorGrid, this.floorGrids[2]);
        }

        public static boolean isHouse(SimpleGrid baseGrid, int gridX, int gridZ) {
            int value = baseGrid.get(gridX, gridZ);
            return value == BaseRoomFlag.CORRIDOR || value == BaseRoomFlag.ROOM || value == BaseRoomFlag.START || value == 4;
        }

        public boolean isRoomId(SimpleGrid baseGrid, int newGridX, int newGridZ, int floorRoomIndex, int value) {
            return (this.floorGrids[floorRoomIndex].get(newGridX, newGridZ) & '\uffff') == value;
        }

        @Nullable
        public BlockDirection get1x2RoomDirection(SimpleGrid baseGrid, int gridX, int gridZ, int floorRoomIndex, int value) {
            for (BlockDirection direction : BlockDirection.getHorizontal()) {
                if (this.isRoomId(baseGrid, gridX + direction.getVector().getX(), gridZ + direction.getVector().getZ(), floorRoomIndex, value)) {
                    return direction;
                }
            }

            return null;
        }

        private void recursiveCorridor(SimpleGrid baseGrid, int gridX, int gridZ, BlockDirection direction, int genDepth) {
            if (genDepth > 0) {
                baseGrid.set(gridX, gridZ, BaseRoomFlag.CORRIDOR);
                baseGrid.setif(gridX + direction.getVector().getX(), gridZ + direction.getVector().getZ(), 0, BaseRoomFlag.CORRIDOR);

                for (int i = 0; i < 8; ++i) {
                    BlockDirection newDirection = BlockDirection.get2d()[this.random.nextInt(4)];
                    if (newDirection != direction.getOpposite() && (newDirection != BlockDirection.EAST || !this.random.nextBoolean())) {
                        int stepX = gridX + direction.getVector().getX();
                        int stepZ = gridZ + direction.getVector().getZ();
                        if (baseGrid.get(stepX + newDirection.getVector().getX(), stepZ + newDirection.getVector().getZ()) == 0 && baseGrid.get(stepX + newDirection.getVector().getX() * 2,
                                stepZ + newDirection.getVector().getZ() * 2) == 0) {
                            this.recursiveCorridor(baseGrid, gridX + direction.getVector().getX() + newDirection.getVector().getX(),
                                    gridZ + direction.getVector().getZ() + newDirection.getVector().getZ(), newDirection, genDepth - 1);
                            break;
                        }
                    }
                }

                BlockDirection cw = direction.getClockWise();
                BlockDirection ccw = direction.getCounterClockWise();
                baseGrid.setif(gridX + cw.getVector().getX(), gridZ + cw.getVector().getZ(), 0, 2);
                baseGrid.setif(gridX + ccw.getVector().getX(), gridZ + ccw.getVector().getZ(), 0, 2);
                baseGrid.setif(gridX + direction.getVector().getX() + cw.getVector().getX(), gridZ + direction.getVector().getZ() + cw.getVector().getZ(), 0, 2);
                baseGrid.setif(gridX + direction.getVector().getX() + ccw.getVector().getX(), gridZ + direction.getVector().getZ() + ccw.getVector().getZ(), 0, 2);
                baseGrid.setif(gridX + direction.getVector().getX() * 2, gridZ + direction.getVector().getZ() * 2, 0, 2);
                baseGrid.setif(gridX + cw.getVector().getX() * 2, gridZ + cw.getVector().getZ() * 2, 0, 2);
                baseGrid.setif(gridX + ccw.getVector().getX() * 2, gridZ + ccw.getVector().getZ() * 2, 0, 2);
            }
        }

        private boolean cleanEdges(SimpleGrid baseGrid) {
            boolean flag = false;

            for (int gridZ = 0; gridZ < baseGrid.height; ++gridZ) {
                for (int gridX = 0; gridX < baseGrid.width; ++gridX) {
                    if (baseGrid.get(gridX, gridZ) == 0) {
                        int adjacentRooms = 0;
                        adjacentRooms = adjacentRooms + (isHouse(baseGrid, gridX + 1, gridZ) ? 1 : 0);
                        adjacentRooms = adjacentRooms + (isHouse(baseGrid, gridX - 1, gridZ) ? 1 : 0);
                        adjacentRooms = adjacentRooms + (isHouse(baseGrid, gridX, gridZ + 1) ? 1 : 0);
                        adjacentRooms = adjacentRooms + (isHouse(baseGrid, gridX, gridZ - 1) ? 1 : 0);
                        if (adjacentRooms >= 3) {
                            baseGrid.set(gridX, gridZ, 2);
                            flag = true;
                        } else if (adjacentRooms == 2) {
                            int diagonalRooms = 0;
                            diagonalRooms = diagonalRooms + (isHouse(baseGrid, gridX + 1, gridZ + 1) ? 1 : 0);
                            diagonalRooms = diagonalRooms + (isHouse(baseGrid, gridX - 1, gridZ + 1) ? 1 : 0);
                            diagonalRooms = diagonalRooms + (isHouse(baseGrid, gridX + 1, gridZ - 1) ? 1 : 0);
                            diagonalRooms = diagonalRooms + (isHouse(baseGrid, gridX - 1, gridZ - 1) ? 1 : 0);
                            if (diagonalRooms <= 1) {
                                baseGrid.set(gridX, gridZ, 2);
                                flag = true;
                            }
                        }
                    }
                }
            }

            return flag;
        }

        private void setupThirdFloor() {
            List<Pair<Integer, Integer>> secret1x2 = new ArrayList<>();
            SimpleGrid secondFloorGrid = this.floorGrids[1];

            for (int gridZ = 0; gridZ < this.thirdFloorGrid.height; ++gridZ) {
                for (int gridX = 0; gridX < this.thirdFloorGrid.width; ++gridX) {
                    int flag = secondFloorGrid.get(gridX, gridZ);
                    int roomSize = flag & RoomGroupFlag.ROOM_SIZE;
                    if (roomSize == RoomGroupFlag._1x2FLAG && (flag & RoomGroupFlag.SECRET) == RoomGroupFlag.SECRET) {
                        secret1x2.add(new Pair<>(gridX, gridZ));
                    }
                }
            }

            if (secret1x2.isEmpty()) {
                this.thirdFloorGrid.set(0, 0, this.thirdFloorGrid.width, this.thirdFloorGrid.height, 5);
            } else {
                Pair<Integer, Integer> randomPos = secret1x2.get(this.random.nextInt(secret1x2.size()));
                int flag = secondFloorGrid.get(randomPos.getFirst(), randomPos.getSecond());
                secondFloorGrid.set(randomPos.getFirst(), randomPos.getSecond(), flag | RoomGroupFlag.STAIRS);
                BlockDirection roomDirection = this.get1x2RoomDirection(this.baseGrid, randomPos.getFirst(), randomPos.getSecond(), 1, flag & '\uffff');
                int gridX = randomPos.getFirst() + roomDirection.getVector().getX();
                int gridZ = randomPos.getSecond() + roomDirection.getVector().getZ();

                for (int thirdFloorX = 0; thirdFloorX < this.thirdFloorGrid.height; ++thirdFloorX) {
                    for (int thirdFloorZ = 0; thirdFloorZ < this.thirdFloorGrid.width; ++thirdFloorZ) {
                        if (!isHouse(this.baseGrid, thirdFloorZ, thirdFloorX)) {
                            this.thirdFloorGrid.set(thirdFloorZ, thirdFloorX, BaseRoomFlag.OUTSIDE);
                        } else if (thirdFloorZ == randomPos.getFirst() && thirdFloorX == randomPos.getSecond()) {
                            this.thirdFloorGrid.set(thirdFloorZ, thirdFloorX, BaseRoomFlag.START);
                        } else if (thirdFloorZ == gridX && thirdFloorX == gridZ) {
                            this.thirdFloorGrid.set(thirdFloorZ, thirdFloorX, BaseRoomFlag.START);
                            this.floorGrids[2].set(thirdFloorZ, thirdFloorX, RoomGroupFlag.ENTRANCE);
                        }
                    }
                }

                List<BlockDirection> unsetDirections = new ArrayList<>();

                for (BlockDirection direction : BlockDirection.getHorizontal()) {
                    if (this.thirdFloorGrid.get(gridX + direction.getVector().getX(), gridZ + direction.getVector().getZ()) == 0) {
                        unsetDirections.add(direction);
                    }
                }

                if (unsetDirections.isEmpty()) {
                    this.thirdFloorGrid.set(0, 0, this.thirdFloorGrid.width, this.thirdFloorGrid.height, BaseRoomFlag.OUTSIDE);
                    secondFloorGrid.set(randomPos.getFirst(), randomPos.getSecond(), flag);
                } else {
                    BlockDirection randomUnsetDirection = unsetDirections.get(this.random.nextInt(unsetDirections.size()));
                    this.recursiveCorridor(this.thirdFloorGrid, gridX + randomUnsetDirection.getVector().getX(), gridZ + randomUnsetDirection.getVector().getZ(), randomUnsetDirection, 4);

                    while (this.cleanEdges(this.thirdFloorGrid)) {
                    }

                }
            }
        }

        private void identifyRooms(SimpleGrid baseGrid, SimpleGrid floorRoom) {
            List<Pair<Integer, Integer>> rooms = new ArrayList<>();

            for (int i = 0; i < baseGrid.height; ++i) {
                for (int j = 0; j < baseGrid.width; ++j) {
                    if (baseGrid.get(j, i) == BaseRoomFlag.ROOM) {
                        rooms.add(new Pair<>(j, i));
                    }
                }
            }

            this.random.shuffle(rooms);
            int roomCount = 10;

            for (Pair<Integer, Integer> tuple : rooms) {
                int gridX = tuple.getFirst();
                int gridZ = tuple.getSecond();
                if (floorRoom.get(gridX, gridZ) == 0) {
                    int minX = gridX;
                    int maxX = gridX;
                    int minZ = gridZ;
                    int maxZ = gridZ;
                    int groupFlag = RoomGroupFlag._1x1FLAG;
                    if (floorRoom.get(gridX + 1, gridZ) == 0 &&
                            floorRoom.get(gridX, gridZ + 1) == 0 &&
                            floorRoom.get(gridX + 1, gridZ + 1) == 0 &&
                            baseGrid.get(gridX + 1, gridZ) == BaseRoomFlag.ROOM &&
                            baseGrid.get(gridX, gridZ + 1) == BaseRoomFlag.ROOM &&
                            baseGrid.get(gridX + 1, gridZ + 1) == BaseRoomFlag.ROOM) {
                        maxX = gridX + 1;
                        maxZ = gridZ + 1;
                        groupFlag = RoomGroupFlag._2x2FLAG;
                    } else if (floorRoom.get(gridX - 1, gridZ) == 0 &&
                            floorRoom.get(gridX, gridZ + 1) == 0 &&
                            floorRoom.get(gridX - 1, gridZ + 1) == 0 &&
                            baseGrid.get(gridX - 1, gridZ) == BaseRoomFlag.ROOM &&
                            baseGrid.get(gridX, gridZ + 1) == BaseRoomFlag.ROOM &&
                            baseGrid.get(gridX - 1, gridZ + 1) == BaseRoomFlag.ROOM) {
                        minX = gridX - 1;
                        maxZ = gridZ + 1;
                        groupFlag = RoomGroupFlag._2x2FLAG;
                    } else if (floorRoom.get(gridX - 1, gridZ) == 0 &&
                            floorRoom.get(gridX, gridZ - 1) == 0 &&
                            floorRoom.get(gridX - 1, gridZ - 1) == 0 &&
                            baseGrid.get(gridX - 1, gridZ) == BaseRoomFlag.ROOM &&
                            baseGrid.get(gridX, gridZ - 1) == BaseRoomFlag.ROOM &&
                            baseGrid.get(gridX - 1, gridZ - 1) == BaseRoomFlag.ROOM) {
                        minX = gridX - 1;
                        minZ = gridZ - 1;
                        groupFlag = RoomGroupFlag._2x2FLAG;
                    } else if (floorRoom.get(gridX + 1, gridZ) == 0 &&
                            baseGrid.get(gridX + 1, gridZ) == BaseRoomFlag.ROOM) {
                        maxX = gridX + 1;
                        groupFlag = RoomGroupFlag._1x2FLAG;
                    } else if (floorRoom.get(gridX, gridZ + 1) == 0 &&
                            baseGrid.get(gridX, gridZ + 1) == BaseRoomFlag.ROOM) {
                        maxZ = gridZ + 1;
                        groupFlag = RoomGroupFlag._1x2FLAG;
                    } else if (floorRoom.get(gridX - 1, gridZ) == 0 &&
                            baseGrid.get(gridX - 1, gridZ) == BaseRoomFlag.ROOM) {
                        minX = gridX - 1;
                        groupFlag = RoomGroupFlag._1x2FLAG;
                    } else if (floorRoom.get(gridX, gridZ - 1) == 0 &&
                            baseGrid.get(gridX, gridZ - 1) == BaseRoomFlag.ROOM) {
                        minZ = gridZ - 1;
                        groupFlag = RoomGroupFlag._1x2FLAG;
                    }

                    int startX = this.random.nextBoolean() ? minX : maxX;
                    int startZ = this.random.nextBoolean() ? minZ : maxZ;
                    int secretFlag = RoomGroupFlag.SECRET;
                    if (!baseGrid.adjacentTo(startX, startZ, BaseRoomFlag.CORRIDOR)) {
                        startX = startX == minX ? maxX : minX;
                        startZ = startZ == minZ ? maxZ : minZ;
                        if (!baseGrid.adjacentTo(startX, startZ, BaseRoomFlag.CORRIDOR)) {
                            startZ = startZ == minZ ? maxZ : minZ;
                            if (!baseGrid.adjacentTo(startX, startZ, BaseRoomFlag.CORRIDOR)) {
                                startX = startX == minX ? maxX : minX;
                                startZ = startZ == minZ ? maxZ : minZ;
                                if (!baseGrid.adjacentTo(startX, startZ, BaseRoomFlag.CORRIDOR)) {
                                    secretFlag = 0;
                                    startX = minX;
                                    startZ = minZ;
                                }
                            }
                        }
                    }

                    for (int groupZ = minZ; groupZ <= maxZ; ++groupZ) {
                        for (int groupX = minX; groupX <= maxX; ++groupX) {
                            if (groupX == startX && groupZ == startZ) {
                                floorRoom.set(groupX, groupZ, RoomGroupFlag.START | secretFlag | groupFlag | roomCount);
                            } else {
                                floorRoom.set(groupX, groupZ, groupFlag | roomCount);
                            }
                        }
                    }

                    ++roomCount;
                }
            }

        }
    }

    static class Placer {
        private final ChunkRand random;
        private int startX;
        private int startZ;

        public Placer(ChunkRand random) {
            this.random = random;
        }

        public void createMansion(BPos start, BlockRotation rotation, List<MansionPiece> mansionPieces, Grid grid) {

            SimpleGrid baseGrid = grid.baseGrid;
            SimpleGrid thirdFloorGrid = grid.thirdFloorGrid;
            this.startX = grid.entranceX + 1;
            this.startZ = grid.entranceZ + 1;

            RoomCollection[] roomCollection = new RoomCollection[]{new FirstFloor(), new SecondFloor(), new ThirdFloor()};

            for (int floorIndex = 0; floorIndex < 3; ++floorIndex) {
                BPos bPos = start.add(0, 8 * floorIndex + (floorIndex == 2 ? 3 : 0), 0);
                SimpleGrid floorGrid = grid.floorGrids[floorIndex];
                SimpleGrid baseFloorGrid = floorIndex == 2 ? thirdFloorGrid : baseGrid;

                List<BlockDirection> doorways = new ArrayList<>();
                for (int gridZ = 0; gridZ < baseFloorGrid.height; ++gridZ) {
                    for (int gridX = 0; gridX < baseFloorGrid.width; ++gridX) {
                        boolean thirdFloorStart = floorIndex == 2 && baseFloorGrid.get(gridX, gridZ) == BaseRoomFlag.START;
                        if (baseFloorGrid.get(gridX, gridZ) == BaseRoomFlag.ROOM || thirdFloorStart) {
                            int gridFlag = floorGrid.get(gridX, gridZ);
                            int roomSize = gridFlag & RoomGroupFlag.ROOM_SIZE;
                            int roomId = gridFlag & '\uffff';
                            doorways.clear();
                            if ((gridFlag & RoomGroupFlag.SECRET) == RoomGroupFlag.SECRET) {
                                for (BlockDirection direction : BlockDirection.getHorizontal()) {
                                    if (baseFloorGrid.get(gridX + direction.getVector().getX(), gridZ + direction.getVector().getZ()) == BaseRoomFlag.CORRIDOR) {
                                        doorways.add(direction);
                                    }
                                }
                            }

                            BlockDirection doorDirection = null;
                            if (!doorways.isEmpty()) {
                                doorDirection = doorways.get(this.random.nextInt(doorways.size()));
                            } else if ((gridFlag & RoomGroupFlag.START) == RoomGroupFlag.START) {
                                doorDirection = BlockDirection.UP;
                            }

                            BPos nextBPos = bPos.relative(rotation.rotate(BlockDirection.SOUTH), 8 + (gridZ - this.startZ) * 8);
                            nextBPos = nextBPos.relative(rotation.rotate(BlockDirection.EAST), -1 + (gridX - this.startX) * 8);

                            if (roomSize == RoomGroupFlag._1x1FLAG) {
                                this.addRoom1x1(mansionPieces, nextBPos, rotation, doorDirection, roomCollection[floorIndex]);
                            } else if (roomSize == RoomGroupFlag._1x2FLAG && doorDirection != null) {
                                BlockDirection roomDirection = grid.get1x2RoomDirection(baseFloorGrid, gridX, gridZ, floorIndex, roomId);
                                boolean flag2 = (gridFlag & RoomGroupFlag.STAIRS) == RoomGroupFlag.STAIRS;
                                this.addRoom1x2(mansionPieces, nextBPos, rotation, roomDirection, doorDirection, roomCollection[floorIndex], flag2);
                            } else if (roomSize == RoomGroupFlag._2x2FLAG && doorDirection != null && doorDirection != BlockDirection.UP) {
                                BlockDirection roomDirection = doorDirection.getClockWise();
                                if (!grid.isRoomId(baseFloorGrid, gridX + roomDirection.getVector().getX(), gridZ + roomDirection.getVector().getZ(), floorIndex, roomId)) {
                                    roomDirection = roomDirection.getOpposite();
                                }
                                this.addRoom2x2(mansionPieces, nextBPos, rotation, roomDirection, doorDirection, roomCollection[floorIndex]);
                            } else if (roomSize == RoomGroupFlag._2x2FLAG && doorDirection == BlockDirection.UP) {
                                this.addRoom2x2Secret(mansionPieces, nextBPos, rotation, roomCollection[floorIndex]);
                            }
                        }
                    }
                }
            }
        }

        private void addRoom1x1(List<MansionPiece> mansionPieces, BPos start, BlockRotation rotation, BlockDirection roomDirection, RoomCollection roomCollection) {
            BlockRotation defaultRotation = BlockRotation.NONE;
            String template = roomCollection.get1x1(this.random);
            if (roomDirection != BlockDirection.EAST) {
                if (roomDirection == BlockDirection.NORTH) {
                    defaultRotation = defaultRotation.getRotated(BlockRotation.COUNTERCLOCKWISE_90);
                } else if (roomDirection == BlockDirection.WEST) {
                    defaultRotation = defaultRotation.getRotated(BlockRotation.CLOCKWISE_180);
                } else if (roomDirection == BlockDirection.SOUTH) {
                    defaultRotation = defaultRotation.getRotated(BlockRotation.CLOCKWISE_90);
                } else {
                    template = roomCollection.get1x1Secret(this.random);
                }
            }

            BPos getZeroPos = MansionPiece.getZeroPositionWithTransform(new BPos(1, 0, 0), BlockMirror.NONE, defaultRotation, 7, 7);
            defaultRotation = defaultRotation.getRotated(rotation);

            getZeroPos = getZeroPos.transform(BlockMirror.NONE, rotation, new BPos(0, getZeroPos.getY(), 0));
            BPos finalBPos = start.add(getZeroPos.getX(), 0, getZeroPos.getZ());
            mansionPieces.add(new MansionPiece(template, finalBPos, defaultRotation, BlockMirror.NONE, roomCollection.getFloorNumber()));

        }

        private void addRoom1x2(List<MansionPiece> mansionPieces, BPos start, BlockRotation rotation, BlockDirection roomDirection, BlockDirection doorDirection, RoomCollection roomCollection, boolean isStairs) {
            if (doorDirection == BlockDirection.EAST && roomDirection == BlockDirection.SOUTH) {
                BPos finalBPos = start.relative(rotation.rotate(BlockDirection.EAST), 1);
                mansionPieces.add(new MansionPiece(roomCollection.get1x2SideEntrance(this.random, isStairs), finalBPos, rotation, BlockMirror.NONE, roomCollection.getFloorNumber()));
            } else if (doorDirection == BlockDirection.EAST && roomDirection == BlockDirection.NORTH) {
                BPos finalBPos = start.relative(rotation.rotate(BlockDirection.EAST), 1);
                finalBPos = finalBPos.relative(rotation.rotate(BlockDirection.SOUTH), 6);
                mansionPieces.add(new MansionPiece(roomCollection.get1x2SideEntrance(this.random, isStairs), finalBPos, rotation, BlockMirror.LEFT_RIGHT, roomCollection.getFloorNumber()));
            } else if (doorDirection == BlockDirection.WEST && roomDirection == BlockDirection.NORTH) {
                BPos finalBPos = start.relative(rotation.rotate(BlockDirection.EAST), 7);
                finalBPos = finalBPos.relative(rotation.rotate(BlockDirection.SOUTH), 6);
                mansionPieces.add(new MansionPiece(roomCollection.get1x2SideEntrance(this.random, isStairs), finalBPos, rotation.getRotated(BlockRotation.CLOCKWISE_180), BlockMirror.NONE, roomCollection.getFloorNumber()));
            } else if (doorDirection == BlockDirection.WEST && roomDirection == BlockDirection.SOUTH) {
                BPos finalBPos = start.relative(rotation.rotate(BlockDirection.EAST), 7);
                mansionPieces.add(new MansionPiece(roomCollection.get1x2SideEntrance(this.random, isStairs), finalBPos, rotation, BlockMirror.FRONT_BACK, roomCollection.getFloorNumber()));
            } else if (doorDirection == BlockDirection.SOUTH && roomDirection == BlockDirection.EAST) {
                BPos finalBPos = start.relative(rotation.rotate(BlockDirection.EAST), 1);
                mansionPieces.add(new MansionPiece(roomCollection.get1x2SideEntrance(this.random, isStairs), finalBPos, rotation.getRotated(BlockRotation.CLOCKWISE_90), BlockMirror.LEFT_RIGHT, roomCollection.getFloorNumber()));
            } else if (doorDirection == BlockDirection.SOUTH && roomDirection == BlockDirection.WEST) {
                BPos finalBPos = start.relative(rotation.rotate(BlockDirection.EAST), 7);
                mansionPieces.add(new MansionPiece(roomCollection.get1x2SideEntrance(this.random, isStairs), finalBPos, rotation.getRotated(BlockRotation.CLOCKWISE_90), BlockMirror.NONE, roomCollection.getFloorNumber()));
            } else if (doorDirection == BlockDirection.NORTH && roomDirection == BlockDirection.WEST) {
                BPos finalBPos = start.relative(rotation.rotate(BlockDirection.EAST), 7);
                finalBPos = finalBPos.relative(rotation.rotate(BlockDirection.SOUTH), 6);
                mansionPieces.add(new MansionPiece(roomCollection.get1x2SideEntrance(this.random, isStairs), finalBPos, rotation.getRotated(BlockRotation.CLOCKWISE_90), BlockMirror.FRONT_BACK, roomCollection.getFloorNumber()));
            } else if (doorDirection == BlockDirection.NORTH && roomDirection == BlockDirection.EAST) {
                BPos finalBPos = start.relative(rotation.rotate(BlockDirection.EAST), 1);
                finalBPos = finalBPos.relative(rotation.rotate(BlockDirection.SOUTH), 6);
                mansionPieces.add(new MansionPiece(roomCollection.get1x2SideEntrance(this.random, isStairs), finalBPos, rotation.getRotated(BlockRotation.COUNTERCLOCKWISE_90), BlockMirror.NONE, roomCollection.getFloorNumber()));
            } else if (doorDirection == BlockDirection.SOUTH && roomDirection == BlockDirection.NORTH) {
                BPos finalBPos = start.relative(rotation.rotate(BlockDirection.EAST), 1);
                finalBPos = finalBPos.relative(rotation.rotate(BlockDirection.NORTH), 8);
                mansionPieces.add(new MansionPiece(roomCollection.get1x2FrontEntrance(this.random, isStairs), finalBPos, rotation, BlockMirror.NONE, roomCollection.getFloorNumber()));
            } else if (doorDirection == BlockDirection.NORTH && roomDirection == BlockDirection.SOUTH) {
                BPos finalBPos = start.relative(rotation.rotate(BlockDirection.EAST), 7);
                finalBPos = finalBPos.relative(rotation.rotate(BlockDirection.SOUTH), 14);
                mansionPieces.add(new MansionPiece(roomCollection.get1x2FrontEntrance(this.random, isStairs), finalBPos, rotation.getRotated(BlockRotation.CLOCKWISE_180), BlockMirror.NONE, roomCollection.getFloorNumber()));
            } else if (doorDirection == BlockDirection.WEST && roomDirection == BlockDirection.EAST) {
                BPos finalBPos = start.relative(rotation.rotate(BlockDirection.EAST), 15);
                mansionPieces.add(new MansionPiece(roomCollection.get1x2FrontEntrance(this.random, isStairs), finalBPos, rotation.getRotated(BlockRotation.CLOCKWISE_90), BlockMirror.NONE, roomCollection.getFloorNumber()));
            } else if (doorDirection == BlockDirection.EAST && roomDirection == BlockDirection.WEST) {
                BPos finalBPos = start.relative(rotation.rotate(BlockDirection.WEST), 7);
                finalBPos = finalBPos.relative(rotation.rotate(BlockDirection.SOUTH), 6);
                mansionPieces.add(new MansionPiece(roomCollection.get1x2FrontEntrance(this.random, isStairs), finalBPos, rotation.getRotated(BlockRotation.COUNTERCLOCKWISE_90), BlockMirror.NONE, roomCollection.getFloorNumber()));
            } else if (doorDirection == BlockDirection.UP && roomDirection == BlockDirection.EAST) {
                BPos finalBPos = start.relative(rotation.rotate(BlockDirection.EAST), 15);
                mansionPieces.add(new MansionPiece(roomCollection.get1x2Secret(this.random), finalBPos, rotation.getRotated(BlockRotation.CLOCKWISE_90), BlockMirror.NONE, roomCollection.getFloorNumber()));
            } else if (doorDirection == BlockDirection.UP && roomDirection == BlockDirection.SOUTH) {
                BPos blockpos = start.relative(rotation.rotate(BlockDirection.EAST), 1);
                blockpos = blockpos.relative(rotation.rotate(BlockDirection.NORTH), 0);
                mansionPieces.add(new MansionPiece(roomCollection.get1x2Secret(this.random), blockpos, rotation, BlockMirror.NONE, roomCollection.getFloorNumber()));
            }

        }

        private void addRoom2x2(List<MansionPiece> mansionPieces, BPos start, BlockRotation rotation, BlockDirection roomDirection, BlockDirection doorDirection, RoomCollection roomCollection) {
            int offsetX = 0;
            int offetZ = 0;
            BlockRotation roomRotation = rotation;
            BlockMirror mirror = BlockMirror.NONE;
            if (doorDirection == BlockDirection.EAST && roomDirection == BlockDirection.SOUTH) {
                offsetX = -7;
            } else if (doorDirection == BlockDirection.EAST && roomDirection == BlockDirection.NORTH) {
                offsetX = -7;
                offetZ = 6;
                mirror = BlockMirror.LEFT_RIGHT;
            } else if (doorDirection == BlockDirection.NORTH && roomDirection == BlockDirection.EAST) {
                offsetX = 1;
                offetZ = 14;
                roomRotation = rotation.getRotated(BlockRotation.COUNTERCLOCKWISE_90);
            } else if (doorDirection == BlockDirection.NORTH && roomDirection == BlockDirection.WEST) {
                offsetX = 7;
                offetZ = 14;
                roomRotation = rotation.getRotated(BlockRotation.COUNTERCLOCKWISE_90);
                mirror = BlockMirror.LEFT_RIGHT;
            } else if (doorDirection == BlockDirection.SOUTH && roomDirection == BlockDirection.WEST) {
                offsetX = 7;
                offetZ = -8;
                roomRotation = rotation.getRotated(BlockRotation.CLOCKWISE_90);
            } else if (doorDirection == BlockDirection.SOUTH && roomDirection == BlockDirection.EAST) {
                offsetX = 1;
                offetZ = -8;
                roomRotation = rotation.getRotated(BlockRotation.CLOCKWISE_90);
                mirror = BlockMirror.LEFT_RIGHT;
            } else if (doorDirection == BlockDirection.WEST && roomDirection == BlockDirection.NORTH) {
                offsetX = 15;
                offetZ = 6;
                roomRotation = rotation.getRotated(BlockRotation.CLOCKWISE_180);
            } else if (doorDirection == BlockDirection.WEST && roomDirection == BlockDirection.SOUTH) {
                offsetX = 15;
                mirror = BlockMirror.FRONT_BACK;
            }

            BPos finalBPos = start.relative(rotation.rotate(BlockDirection.EAST), offsetX);
            finalBPos = finalBPos.relative(rotation.rotate(BlockDirection.SOUTH), offetZ);
            mansionPieces.add(new MansionPiece(roomCollection.get2x2(this.random), finalBPos, roomRotation, mirror, roomCollection.getFloorNumber()));
        }

        private void addRoom2x2Secret(List<MansionPiece> mansionPieces, BPos start, BlockRotation rotation, RoomCollection roomCollection) {
            BPos finalBPos = start.relative(rotation.rotate(BlockDirection.EAST), 1);
            mansionPieces.add(new MansionPiece(roomCollection.get2x2Secret(this.random), finalBPos, rotation, BlockMirror.NONE, roomCollection.getFloorNumber()));
        }

    }

    abstract static class RoomCollection {
        private RoomCollection() {
        }

        public abstract int getFloorNumber();

        public abstract String get1x1(ChunkRand random);

        public abstract String get1x1Secret(ChunkRand random);

        public abstract String get1x2SideEntrance(ChunkRand random, boolean isStairs);

        public abstract String get1x2FrontEntrance(ChunkRand random, boolean isStairs);

        public abstract String get1x2Secret(ChunkRand random);

        public abstract String get2x2(ChunkRand random);

        public abstract String get2x2Secret(ChunkRand random);
    }

    static class FirstFloor extends RoomCollection {
        private FirstFloor() {
        }

        @Override
        public int getFloorNumber() {
            return 0;
        }

        @Override
        public String get1x1(ChunkRand random) {
            return "1x1_a" + (random.nextInt(5) + 1);
        }

        @Override
        public String get1x1Secret(ChunkRand random) {
            return "1x1_as" + (random.nextInt(4) + 1);
        }

        @Override
        public String get1x2SideEntrance(ChunkRand random, boolean isStairs) {
            return "1x2_a" + (random.nextInt(9) + 1);
        }

        @Override
        public String get1x2FrontEntrance(ChunkRand random, boolean isStairs) {
            return "1x2_b" + (random.nextInt(5) + 1);
        }

        @Override
        public String get1x2Secret(ChunkRand random) {
            return "1x2_s" + (random.nextInt(2) + 1);
        }

        @Override
        public String get2x2(ChunkRand random) {
            return "2x2_a" + (random.nextInt(4) + 1);
        }

        @Override
        public String get2x2Secret(ChunkRand random) {
            return "2x2_s1";
        }
    }

    static class SecondFloor extends RoomCollection {
        private SecondFloor() {
        }

        @Override
        public int getFloorNumber() {
            return 1;
        }

        @Override
        public String get1x1(ChunkRand random) {
            return "1x1_b" + (random.nextInt(4) + 1);
        }

        @Override
        public String get1x1Secret(ChunkRand random) {
            return "1x1_as" + (random.nextInt(4) + 1);
        }

        @Override
        public String get1x2SideEntrance(ChunkRand random, boolean isStairs) {
            return isStairs ? "1x2_c_stairs" : "1x2_c" + (random.nextInt(4) + 1);
        }

        @Override
        public String get1x2FrontEntrance(ChunkRand random, boolean isStairs) {
            return isStairs ? "1x2_d_stairs" : "1x2_d" + (random.nextInt(5) + 1);
        }

        @Override
        public String get1x2Secret(ChunkRand random) {
            return "1x2_se" + (random.nextInt(1) + 1);
        }

        @Override
        public String get2x2(ChunkRand random) {
            return "2x2_b" + (random.nextInt(5) + 1);
        }

        @Override
        public String get2x2Secret(ChunkRand random) {
            return "2x2_s1";
        }
    }

    static class ThirdFloor extends SecondFloor {
        private ThirdFloor() {
        }

        @Override
        public int getFloorNumber() {
            return 2;
        }
    }

    public static final Map<String, String> COMMON_NAMES = Map.ofEntries(
            Map.entry("1x1_a1", "Flower room"),
            Map.entry("1x1_a2", "Pumpkin ring room"),
            Map.entry("1x1_a3", "Office"),
            Map.entry("1x1_a4", "Checkerboard room"),
            Map.entry("1x1_a5", "White tulip sanctuary"),
            Map.entry("1x1_as1", "X room"),
            Map.entry("1x1_as2", "Spider room"),
            Map.entry("1x1_as3", "Obsidian room"),
            Map.entry("1x1_as4", "Birch pillar room"),
            Map.entry("1x1_b1", "Birch arch room"),
            Map.entry("1x1_b2", "Small dining room"),
            Map.entry("1x1_b3", "Single bed bedroom"),
            Map.entry("1x1_b4", "Small library"),
            Map.entry("1x1_b5", "Allium room"),
            Map.entry("1x2_a1", "Gray banner room"),
            Map.entry("1x2_a2", "Wheat farm"),
            Map.entry("1x2_a3", "Forge room"),
            Map.entry("1x2_a4", "Sapling farm"),
            Map.entry("1x2_a6", "Tree-chopping room"),
            Map.entry("1x2_a7", "Mushroom farm"),
            Map.entry("1x2_a8", "Dual-staged farm"),
            Map.entry("1x2_a9", "Small storage room"),
            Map.entry("1x2_b1", "Redstone jail"),
            Map.entry("1x2_b2", "Small jail"),
            Map.entry("1x2_b3", "Wood arch hallway"),
            Map.entry("1x2_b4", "Winding stairway room"),
            Map.entry("1x2_b5", "Illager head room"),
            Map.entry("1x2_c_stairs", "Curved staircase"),
            Map.entry("1x2_c1", "Medium dining room"),
            Map.entry("1x2_c2", "Double bed bedroom"),
            Map.entry("1x2_c3", "Triple bed bedroom"),
            Map.entry("1x2_c4", "Medium library"),
            Map.entry("1x2_d_stairs", "Straight staircase"),
            Map.entry("1x2_d1", "Master bedroom"),
            Map.entry("1x2_d2", "Bedroom with loft"),
            Map.entry("1x2_d3", "Ritual room"),
            Map.entry("1x2_d4", "Cat statue room"),
            Map.entry("1x2_d5", "Chicken statue room"),
            Map.entry("1x2_s1", "Clean chest room"),
            Map.entry("1x2_s2", "Fake End portal room"),
            Map.entry("1x2_se1", "Attic room"),
            Map.entry("2x2_a1", "Large jail"),
            Map.entry("2x2_a2", "Large storage room"),
            Map.entry("2x2_a3", "Illager statue room"),
            Map.entry("2x2_a4", "Nature room"),
            Map.entry("2x2_b1", "Large dining room"),
            Map.entry("2x2_b2", "Conference room"),
            Map.entry("2x2_b3", "Large library"),
            Map.entry("2x2_b4", "Map room"),
            Map.entry("2x2_b5", "Arena room"),
            Map.entry("2x2_s1", "Lava room")
    );

    static class BaseRoomFlag {
        static final int OUTSIDE = 5;
        static final int UNSET = 0;
        static final int CORRIDOR = 1;
        static final int ROOM = 2;
        static final int START = 3;
    }

    static class RoomGroupFlag {
        static final int ROOM_ID = 0xFFFF;
        static final int ROOM_SIZE = 0xF0000;

        static final int _1x1FLAG = 0x10000;
        static final int _1x2FLAG = 0x20000;
        static final int _2x2FLAG = 0x40000;

        static final int START = 0x100000;
        static final int SECRET = 0x200000;
        static final int STAIRS = 0x400000;
        static final int ENTRANCE = 0x800000;
    }
}