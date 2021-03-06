package com.magicbox.redio.utils;

import java.util.ArrayList;
import java.util.HashMap;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.oredict.ShapedOreRecipe;

import com.magicbox.redio.emulator.IPacketRouterNode;
import com.magicbox.redio.entities.EntityBusCable;
import com.magicbox.redio.entities.EntityProcessor;
import com.magicbox.redio.script.objects.RedNullObject;
import com.magicbox.redio.script.objects.RedObject;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;

public class Utils
{
	private static HashMap<Side, HashMap<String, IPacketRouterNode>> mappedNodes = new HashMap<Side, HashMap<String, IPacketRouterNode>>();

	static
	{
		mappedNodes.put(Side.CLIENT, new HashMap<String, IPacketRouterNode>());
		mappedNodes.put(Side.SERVER, new HashMap<String, IPacketRouterNode>());
	}

	public static int getPlayerFacing(EntityLivingBase player)
	{
		return MathHelper.floor_double(player.rotationYaw * 4.0F / 360.0F + 0.5d) & 0x03;
	}

	public static void registerRouter(String name, IPacketRouterNode node)
	{
		mappedNodes.get(FMLCommonHandler.instance().getEffectiveSide()).put(name, node);
	}

	public static void unregisterRouter(String name, IPacketRouterNode node)
	{
		if (mappedNodes.get(FMLCommonHandler.instance().getEffectiveSide()).containsKey(name))
			if (mappedNodes.get(FMLCommonHandler.instance().getEffectiveSide()).get(name) == node)
				mappedNodes.get(FMLCommonHandler.instance().getEffectiveSide()).remove(name);
	}

	public static void addCraftingRecipe(Block result, int count, Object... recipes)
	{
		if (recipes.length == 9)
		{
			char name = 'A';
			String recipe = "";
			ItemStack stack = new ItemStack(result, count);
			ArrayList<Object> args = new ArrayList<Object>();
			HashMap<Object, Character> map = new HashMap<Object, Character>();

			for (Object item : recipes)
			{
				if (item == null)
				{
					recipe += " ";
				}
				else
				{
					char current = name;

					if (!map.containsKey(item))
						map.put(item, name++);
					else
						current = map.get(item);

					recipe += current;
				}
			}

			args.add(recipe.substring(0, 3));
			args.add(recipe.substring(3, 6));
			args.add(recipe.substring(6, 9));

			for (Object item : map.keySet())
			{
				if (item != null)
				{
					args.add(map.get(item));
					args.add(item);
				}
			}

			CraftingManager.getInstance().getRecipeList().add(new ShapedOreRecipe(stack, args.toArray()).setMirrored(true));
		}
	}

	public static String getRouterName(String prefix)
	{
		int i = 1;

		while (mappedNodes.get(FMLCommonHandler.instance().getEffectiveSide()).containsKey(prefix + i))
			i++;

		return prefix + i;
	}

	public static boolean isEntityPowered(TileEntity entity)
	{
		return entity.getWorldObj().isBlockIndirectlyGettingPowered(entity.xCoord, entity.yCoord, entity.zCoord);
	}

	public static boolean isRouterNameValid(String name)
	{
		return !mappedNodes.get(FMLCommonHandler.instance().getEffectiveSide()).containsKey(name);
	}

	public static boolean hasProcessorAround(World world, int x, int y, int z)
	{
		// @formatter:off
		TileEntity [] entities = new TileEntity[]
		{
			world.getTileEntity(x - 1, y, z),
			world.getTileEntity(x + 1, y, z),
			world.getTileEntity(x, y, z - 1),
			world.getTileEntity(x, y, z + 1),
			world.getTileEntity(x, y + 1, z),
		};

		// @formatter:on
		for (TileEntity entity : entities)
			if (entity instanceof EntityProcessor)
				return true;

		return false;
	}

	public static RedObject broadcastProcessorPacket(EntityProcessor processor, String destination, RedObject packet)
	{
		for (EntityBusCable cable : processor.getConnectedCables())
		{
			RedObject result = cable.dispatchPacket(processor, processor, destination, packet);

			if (result != null && !result.isNull())
				return result;
		}

		return RedNullObject.nullObject;
	}
}
