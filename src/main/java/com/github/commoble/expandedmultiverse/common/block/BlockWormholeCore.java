package com.github.commoble.expandedmultiverse.common.block;

import javax.annotation.Nullable;

import com.github.commoble.expandedmultiverse.common.ConfigMultiverse;
import com.github.commoble.expandedmultiverse.common.capability.portal_loader.IPortalLoaderCapability;
import com.github.commoble.expandedmultiverse.common.capability.portal_loader.PortalLoaderCapability;
import com.github.commoble.expandedmultiverse.common.capability.portal_loader.PortalLoaderProvider;
import com.github.commoble.expandedmultiverse.common.item.ItemLedger;
import com.github.commoble.expandedmultiverse.common.multiverse.DimensionLedger;
import com.github.commoble.expandedmultiverse.common.multiverse.PerpendicularTeleporter;
import com.github.commoble.expandedmultiverse.common.tileentity.TileEntityWormholeCore;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ITeleporter;

public class BlockWormholeCore extends BlockContainer
{

	public BlockWormholeCore()
	{
		super(Material.PORTAL);
		this.setCreativeTab(ItemLedger.emtab);
		this.setSoundType(SoundType.GLASS);
		this.lightValue = 15;
		this.setBlockUnbreakable();
		// TODO Auto-generated constructor stub
	}

	@Override
	public TileEntity createNewTileEntity(World worldIn, int meta)
	{
		// TODO Auto-generated method stub
		return new TileEntityWormholeCore();
	}

    /**
     * Called when the block is right clicked by a player.
     */
    /*(public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
    {
    	if (world.isRemote || !(playerIn instanceof EntityPlayerMP))
    	{
    		return true;	// if client, ignore
    	}
    	else
    	{
    		if (playerIn.isRiding() || playerIn.isBeingRidden())
    		{
    			return false;	// if player is attached to another entity, ignore (must dismount before using portal)
    		}
    		else
    		{
    			world.notifyBlockUpdate(pos, this.getDefaultState(), this.getDefaultState(), 3);
                playerIn.changeDimension(this.getDestinationDimensionID(world, pos), this.getTeleporter(pos));
                return true;
    		}
    	}
    }*/

    // get the ID of the destination world
    // based on the world the traveller is standing in *before* travel
    public int getDestinationDimensionID(World world, BlockPos pos)
    {
    	// if this is the main world, go to another world
    	if (world.provider.getDimension() == 0)
    	{
    		int index = (Math.abs(pos.hashCode())) % ConfigMultiverse.perpendicular_universe_count;//ConfigMultiverse.perpendicular_universe_count;
    		return DimensionLedger.getPerpendicularUniverseID(index);
    	}
    	else	// if this is ANY OTHER WORLD, go back to the main world
    	{
    		return 0;
    	}
    }
    
    public ITeleporter getTeleporter(BlockPos pos)
    {
    	return new PerpendicularTeleporter(pos);
    }
    
    public static void activateWormhole(World world, BlockPos pos)
    {
    	TileEntity te = world.getTileEntity(pos);
    	if (te instanceof TileEntityWormholeCore) // just in case
    	{
    		((TileEntityWormholeCore)te).activateWormhole();
    	}
    }

    /**
     * Called When an Entity Collided with the Block
     */
    public void onEntityCollidedWithBlock(World worldIn, BlockPos pos, IBlockState state, Entity entityIn)
    {
        if (entityIn instanceof EntityPlayer)
        {
        	TileEntityWormholeCore te = (TileEntityWormholeCore)worldIn.getTileEntity(pos);
        	if (te.getIsActive())
        	{
            	IPortalLoaderCapability cap = entityIn.getCapability(PortalLoaderProvider.PORTAL_LOADER_CAP, null);
            	int ticksInPortal = cap.getTicksInPortal();
            	int cooldown = cap.getCooldownTicks();
            	
            	// if the player is standing in a portal waiting for a new teleport
            	if (cooldown >= 0)
            	{
            		// reset the cooldown
            		cap.setCooldownTicks(PortalLoaderCapability.TICKS_BEFORE_EFFECT_DISSIPATES);
            		if (ticksInPortal < PortalLoaderCapability.TICKS_TO_INITIATE_TELEPORT)
            		{
            			cap.addTicksInPortal(1);
            		}
            		else
            		{	
            			// if on server and entity is not attached to another entity
            			if (!worldIn.isRemote
            					&& !entityIn.isRiding()
            					&& !entityIn.isBeingRidden())
            			{

                            // make sure old player doesn't reteleport
                            cap.setCooldownTicks(-PortalLoaderCapability.TICKS_TO_RESET_TELEPORT);
                            
                            entityIn.changeDimension(this.getDestinationDimensionID(worldIn, pos), this.getTeleporter(pos));
            			}
            		}
            		
            		
            	}
            	else // if the player is standing in a portal after just having teleported
            	{
            		cap.setCooldownTicks(-PortalLoaderCapability.TICKS_TO_RESET_TELEPORT);
            		cap.setTicksInPortal(PortalLoaderCapability.TICKS_TO_INITIATE_TELEPORT - 1);
            	}
        	}
        }
    }
    
    @Override
    public MapColor getMapColor(IBlockState state, IBlockAccess worldIn, BlockPos pos)
    {
    	return MapColor.RED;	// shouldn't be visible on maps, but if it does make it stand out so it's noticed and fixed
    }
    
    @Override
    public boolean isReplaceable(IBlockAccess worldIn, BlockPos pos)
    {
    	return false;
    }

    @Nullable
    public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess worldIn, BlockPos pos)
    {
        return NULL_AABB;
    }

	@Override
	public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer,
			ItemStack stack)
	{
		// TODO Auto-generated method stub
		super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
	}
    


    /**
     * Get the geometry of the queried face at the given position and state. This is used to decide whether things like
     * buttons are allowed to be placed on the face, or how glass panes connect to the face, among other things.
     * <p>
     * Common values are {@code SOLID}, which is the default, and {@code UNDEFINED}, which represents something that
     * does not fit the other descriptions and will generally cause other things not to connect to the face.
     * 
     * @return an approximation of the form of the given face
     */
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face)
    {
        return BlockFaceShape.UNDEFINED;
    }
    
    public EnumBlockRenderType getRenderType(IBlockState state)
    {
        return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
    }
    
    public boolean isOpaqueCube(IBlockState state)
    {
    	return false;
    }
    
    public boolean isFullCube(IBlockState state)
    {
    	return false;
    }
}