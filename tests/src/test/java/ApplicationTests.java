import io.anuke.arc.ApplicationCore;
import io.anuke.arc.backends.headless.HeadlessApplication;
import io.anuke.arc.backends.headless.HeadlessApplicationConfiguration;
import io.anuke.arc.math.geom.Point2;
import io.anuke.arc.util.Log;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.UnitTypes;
import io.anuke.mindustry.content.blocks.*;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.core.Logic;
import io.anuke.mindustry.core.NetServer;
import io.anuke.mindustry.core.World;
import io.anuke.mindustry.entities.traits.BuilderTrait.BuildRequest;
import io.anuke.mindustry.entities.units.BaseUnit;
import io.anuke.mindustry.entities.units.types.Spirit;
import io.anuke.mindustry.game.Content;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.io.BundleLoader;
import io.anuke.mindustry.io.SaveIO;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.type.ContentType;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Recipe;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Edges;
import io.anuke.mindustry.world.Tile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.anuke.mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

public class ApplicationTests{

    @BeforeAll
    static void launchApplication(){
        try{
            boolean[] begins = {false};
            Throwable[] exceptionThrown = {null};
            Log.setUseColors(false);

            ApplicationCore core = new ApplicationCore(){
                @Override
                public void setup(){
                    Vars.init();

                    headless = true;

                    BundleLoader.load();
                    content.load();
                    content.initialize(Content::init);

                    add(logic = new Logic());
                    add(world = new World());
                    add(netServer = new NetServer());
                }

                @Override
                public void init(){
                    super.init();
                    begins[0] = true;
                }
            };

            HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();

            new HeadlessApplication(core, config);

            for(Thread thread : Thread.getAllStackTraces().keySet()){
                if(thread.getName().equals("HeadlessApplication")){
                    thread.setUncaughtExceptionHandler((t, throwable) -> exceptionThrown[0] = throwable);
                    break;
                }
            }

            while(!begins[0]){
                if(exceptionThrown[0] != null){
                    fail(exceptionThrown[0]);
                }
                Thread.sleep(10);
            }
        }catch(Throwable r){
            fail(r);
        }
    }

    @BeforeEach
    void resetWorld(){
        Time.setDeltaProvider(() ->  1f);
        logic.reset();
        state.set(State.menu);
    }

    @Test
    void initialization(){
        assertNotNull(logic);
        assertNotNull(world);
        assertTrue(content.getContentMap().length > 0);
    }

    @Test
    void loadSector(){
        world.sectors.createSector(0, 0);
        world.sectors.playSector(world.sectors.get(0, 0));
    }

    @Test
    void playMap(){
        assertTrue(world.maps.all().size > 0);

        world.loadMap(world.maps.all().first());
    }

    @Test
    void spawnWaves(){
        world.loadMap(world.maps.all().first());
        logic.runWave();
        unitGroups[waveTeam.ordinal()].updateEvents();
        assertFalse(unitGroups[waveTeam.ordinal()].isEmpty());
    }

    @Test
    void createMap(){
        assertTrue(world.maps.all().size > 0);

        Tile[][] tiles = world.createTiles(8, 8);

        world.beginMapLoad();
        for(int x = 0; x < tiles.length; x++){
            for(int y = 0; y < tiles[0].length; y++){
                tiles[x][y] = new Tile(x, y, (byte)0, (byte)0);
            }
        }
        world.endMapLoad();
    }

    @Test
    void multiblock(){
        createMap();
        int bx = 4;
        int by = 4;
        world.setBlock(world.tile(bx, by), StorageBlocks.core, Team.blue);
        assertEquals(world.tile(bx, by).getTeam(), Team.blue);
        for(int x = bx-1; x <= bx + 1; x++){
            for(int y = by-1; y <= by + 1; y++){
                if(x == bx && by == y){
                    assertEquals(world.tile(x, y).block(), StorageBlocks.core);
                }else{
                    assertTrue(world.tile(x, y).block() == Blocks.blockpart && world.tile(x, y).getLinked() == world.tile(bx, by));
                }
            }
        }
    }

    @Test
    void blockInventories(){
        multiblock();
        Tile tile = world.tile(4, 4);
        tile.entity.items.add(Items.coal, 5);
        tile.entity.items.add(Items.titanium, 50);
        assertEquals(tile.entity.items.total(), 55);
        tile.entity.items.remove(Items.phasefabric, 10);
        tile.entity.items.remove(Items.titanium, 10);
        assertEquals(tile.entity.items.total(), 45);
    }

    @Test
    void timers(){
        boolean[] ran = {false};
        Time.run(1.9999f, () -> ran[0] = true);

        Time.update();
        assertFalse(ran[0]);
        Time.update();
        assertTrue(ran[0]);
    }

    @Test
    void save(){
        assertTrue(world.maps.all().size > 0);

        world.loadMap(world.maps.all().first());
        SaveIO.saveToSlot(0);
    }

    @Test
    void load(){
        assertTrue(world.maps.all().size > 0);

        world.loadMap(world.maps.all().first());
        Map map = world.getMap();

        SaveIO.saveToSlot(0);
        resetWorld();
        SaveIO.loadFromSlot(0);

        assertEquals(world.getMap(), map);
        assertEquals(world.width(), map.meta.width);
        assertEquals(world.height(), map.meta.height);
    }

    @Test
    void inventoryDeposit(){
        depositTest(CraftingBlocks.smelter, Items.copper);
        depositTest(StorageBlocks.vault, Items.copper);
        depositTest(PowerBlocks.thoriumReactor, Items.thorium);
    }

    @Test
    void edges(){
        Point2[] edges = Edges.getEdges(1);
        assertEquals(edges[0], new Point2(1, 0));
        assertEquals(edges[1], new Point2(0, 1));
        assertEquals(edges[2], new Point2(-1, 0));
        assertEquals(edges[3], new Point2(0, -1));

        Point2[] edges2 = Edges.getEdges(2);
        assertEquals(8, edges2.length);
    }

    @Test
    void buildingOverlap(){
        initBuilding();

        Spirit d1 = (Spirit) UnitTypes.spirit.create(Team.blue);
        Spirit d2 = (Spirit) UnitTypes.spirit.create(Team.blue);

        d1.set(10f, 20f);
        d2.set(10f, 20f);

        d1.addBuildRequest(new BuildRequest(0, 0, 0, Recipe.getByResult(DefenseBlocks.copperWallLarge)));
        d2.addBuildRequest(new BuildRequest(1, 1, 0, Recipe.getByResult(DefenseBlocks.copperWallLarge)));

        Time.setDeltaProvider(() -> 9999999f);
        d1.updateBuilding(d1);
        d2.updateBuilding(d2);

        assertEquals(DefenseBlocks.copperWallLarge, world.tile(0, 0).block());
        assertEquals(Blocks.air, world.tile(2, 2).block());
        assertEquals(Blocks.blockpart, world.tile(1, 1).block());
    }

    @Test
    void buildingDestruction(){
        initBuilding();

        Spirit d1 = (Spirit) UnitTypes.spirit.create(Team.blue);
        Spirit d2 = (Spirit) UnitTypes.spirit.create(Team.blue);

        d1.set(10f, 20f);
        d2.set(10f, 20f);

        d1.addBuildRequest(new BuildRequest(0, 0, 0, Recipe.getByResult(DefenseBlocks.copperWallLarge)));
        d2.addBuildRequest(new BuildRequest(1, 1));

        Time.setDeltaProvider(() -> 3f);
        d1.updateBuilding(d1);
        Time.setDeltaProvider(() -> 1f);
        d2.updateBuilding(d2);

        assertEquals(content.getByName(ContentType.block, "build2"), world.tile(0, 0).block());

        Time.setDeltaProvider(() -> 9999f);

        d1.updateBuilding(d1);
        d2.updateBuilding(d2);

        assertEquals(Blocks.air, world.tile(0, 0).block());
        assertEquals(Blocks.air, world.tile(2, 2).block());
        assertEquals(Blocks.air, world.tile(1, 1).block());
    }

    void initBuilding(){
        createMap();

        Tile core = world.tile(5, 5);
        world.setBlock(core, StorageBlocks.core, Team.blue);
        for(Item item : content.items()){
            core.entity.items.set(item, 3000);
        }

        assertEquals(core, state.teams.get(Team.blue).cores.first());
    }

    void depositTest(Block block, Item item){
        BaseUnit unit = UnitTypes.spirit.create(Team.none);
        Tile tile = new Tile(0, 0, Blocks.air.id, block.id);
        int capacity = tile.block().itemCapacity;

        int deposited = tile.block().acceptStack(item, capacity - 1, tile, unit);
        assertEquals(capacity - 1, deposited);

        tile.block().handleStack(item, capacity - 1, tile, unit);
        assertEquals(tile.entity.items.get(item), capacity - 1);

        int overflow = tile.block().acceptStack(item, 10, tile, unit);
        assertEquals(1, overflow);

        tile.block().handleStack(item, 1, tile, unit);
        assertEquals(capacity, tile.entity.items.get(item));
    }
}