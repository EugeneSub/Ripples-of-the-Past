package com.github.standobyte.jojo.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.standobyte.jojo.entity.damaging.projectile.HGEmeraldEntity;
import com.github.standobyte.jojo.entity.damaging.projectile.ownerbound.HGBarrierEntity;
import com.github.standobyte.jojo.entity.stand.StandStatFormulas;
import com.github.standobyte.jojo.entity.stand.stands.HierophantGreenEntity;
import com.github.standobyte.jojo.init.ModSounds;
import com.github.standobyte.jojo.power.stand.IStandPower;
import com.github.standobyte.jojo.power.stand.StandUtil;
import com.github.standobyte.jojo.util.utils.JojoModUtil;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;

public class HGBarriersNet {
	private Map<HGBarrierEntity, ShootingPoints> placedBarriers = new HashMap<>();
	private GraphAdjacencyList<Vector3d> closePoints = new GraphAdjacencyList<>();
	private double lastShotGap = -1;
    private boolean canShoot;

	public void tick() {
        Iterator<Map.Entry<HGBarrierEntity, ShootingPoints>> iter = placedBarriers.entrySet().iterator();
        while (iter.hasNext()) {
            HGBarrierEntity barrier = iter.next().getKey();
            if (!barrier.isAlive() || barrier.wasRipped()) {
            	onRemoved(barrier);
                iter.remove();
            }
        }
        canShoot = true;
	}
	
	public void add(HGBarrierEntity barrier) {
		placedBarriers.put(barrier, generateShootingPoints(barrier));
		onUpdate();
	}
	
	private static final double SHOOTING_POINTS_GAP = 8;
	private ShootingPoints generateShootingPoints(HGBarrierEntity entity) {
		Vector3d posA = entity.position();
		Vector3d posB = entity.getOriginPoint(1.0F);
		Vector3d vecAToB = posB.subtract(posA);
		
		List<Vector3d> shootingPoints = new ArrayList<>();
		if (vecAToB.lengthSqr() <= SHOOTING_POINTS_GAP * SHOOTING_POINTS_GAP * 4) {
			shootingPoints.add(posA.add(vecAToB.scale(0.5)));
		}
		else {
			int steps = MathHelper.floor(vecAToB.length() / SHOOTING_POINTS_GAP);
			Vector3d nextPoint = posA;
			Vector3d stepVec = vecAToB.normalize().scale(SHOOTING_POINTS_GAP);
			for (int i = 0; i < steps; i++) {
				nextPoint = nextPoint.add(stepVec);
				shootingPoints.add(nextPoint);
			}
		}

		return new ShootingPoints(shootingPoints);
	}
	
	private void onRemoved(HGBarrierEntity barrier) {
		
		onUpdate();
	}
	
	private void onUpdate() {
		if (lastShotGap > -1) {
			closePoints.clear();
			lastShotGap = -1;
		}
	}
	
	public int getSize() {
		return placedBarriers.size();
	}

    public void shootEmeraldsFromBarriers(IStandPower standPower, HierophantGreenEntity stand, 
    		Vector3d targetPos, int tick, double maxEmeralds, float staminaPerEmerald, double minGap, boolean breakBlocks) {
    	if (!canShoot) return;
    	List<Vector3d> shootingPoints = placedBarriers.values().stream().flatMap(points -> 
    	points.shootingPoints.stream()).collect(Collectors.toCollection(LinkedList::new));
    	if (lastShotGap != minGap) {
    		closePoints.create((pointA, pointB) -> pointA.distanceToSqr(pointB) < minGap * minGap, shootingPoints);
    		lastShotGap = minGap;
    	}

    	Set<Vector3d> pointsToShootThisTick = new HashSet<>();
    	JojoModUtil.doFractionTimes(() -> {
    		Vector3d point = shootingPoints.stream().min(Comparator.comparingDouble(p -> p.distanceToSqr(targetPos))).get();
    		pointsToShootThisTick.add(point);
    		shootingPoints.remove(point);
    		closePoints.getAllAdjacent(point).forEach(closePoint -> shootingPoints.remove(closePoint));
    	}, maxEmeralds, () -> shootingPoints.isEmpty());
    	
    	for (Vector3d point : pointsToShootThisTick) {
    		if (!standPower.consumeStamina(staminaPerEmerald)) {
    			break;
    		}
    		JojoModUtil.doFractionTimes(() -> shootEmerald(stand, point, targetPos, tick == 0, breakBlocks), 
    				StandStatFormulas.projectileFireRateScaling(stand, standPower));
    	}
    	canShoot = false;
    }
    
    public enum PointsChoice {
    	RANDOM,
    	CLOSEST
    }
    
    private void shootEmerald(HierophantGreenEntity stand, Vector3d shootingPos, Vector3d targetPos, boolean playSound, boolean breakBlocks) {
    	if (!stand.level.isClientSide()) {
    		HGEmeraldEntity emeraldEntity = new HGEmeraldEntity(stand, stand.level, null);
    		emeraldEntity.setPos(shootingPos.x, shootingPos.y, shootingPos.z);
    		emeraldEntity.setConcentrated(breakBlocks);
    		Vector3d shootVec = targetPos.subtract(shootingPos);
    		emeraldEntity.shoot(shootVec.x, shootVec.y, shootVec.z, 1F, stand.getProjectileInaccuracy(2.0F));
    		emeraldEntity.setDamageFactor(0.75F);
    		stand.addProjectile(emeraldEntity);
    		if (playSound) {
    			JojoModUtil.playSound(stand.level, null, shootingPos.x, shootingPos.y, shootingPos.z, 
    					ModSounds.HIEROPHANT_GREEN_EMERALD_SPLASH.get(), 
    					stand.getSoundSource(), 1.0F, 1.0F, StandUtil::shouldHearStands);
    		}
    	}
    }
    
    private class ShootingPoints {
    	private final List<Vector3d> shootingPoints;
    	
    	private ShootingPoints(List<Vector3d> shootingPoints) {
    		this.shootingPoints = shootingPoints;
    	}
    }
}
