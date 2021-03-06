package io.anuke.mindustry.world.blocks;

import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.world.Tile;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.math.Mathf;

public class OreBlock extends Floor{
    public Floor base;

    public OreBlock(Item ore, Floor base){
        super("ore-" + ore.name + "-" + base.name);
        this.formalName = ore.localizedName() + " " + base.formalName;
        this.drops = new ItemStack(ore, 1);
        this.base = base;
        this.variants = 3;
        this.minimapColor = ore.color;
        this.edge = base.name;
    }

    @Override
    public String getDisplayName(Tile tile){
        return drops.item.localizedName();
    }

    @Override
    public TextureRegion getEditorIcon(){
        if(editorIcon == null){
            editorIcon = variantRegions[0];
        }
        return editorIcon;
    }

    @Override
    public void draw(Tile tile){
        Draw.rect(variantRegions[Mathf.randomSeed(tile.pos(), 0, Math.max(0, variantRegions.length - 1))], tile.worldx(), tile.worldy());

        drawEdges(tile, false);
    }

    @Override
    public void drawNonLayer(Tile tile){
        Mathf.random.setSeed(tile.pos());

        base.drawEdges(tile, true);
    }

}
