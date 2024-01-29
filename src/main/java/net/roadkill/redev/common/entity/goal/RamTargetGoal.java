package net.roadkill.redev.common.entity.goal;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraftforge.network.PacketDistributor;
import net.roadkill.redev.common.entity.HoveringInfernoEntity;
import net.roadkill.redev.core.network.ReDevPacketHandler;
import net.roadkill.redev.core.network.message.ParticlesMessage;

public class RamTargetGoal extends Goal
{
    HoveringInfernoEntity entity;
    int lastUsedTimestamp = 0;
    int ticksRemaining = 0;

    public RamTargetGoal(HoveringInfernoEntity entity)
    {   this.entity = entity;
    }

    @Override
    public boolean canUse()
    {   return entity.getRandom().nextInt(2) == 0 && entity.getTarget() != null && entity.getTarget().isAlive()
            && entity.getAttackPhase() == HoveringInfernoEntity.AttackPhase.NONE
            && entity.getAttackCooldown() <= 0 && entity.tickCount - lastUsedTimestamp > 100;
    }

    @Override
    public boolean canContinueToUse()
    {   return ticksRemaining > 0 && entity.getAttackPhase() == HoveringInfernoEntity.AttackPhase.BLOCKING
            && entity.getTarget() != null && entity.getTarget().isAlive();
    }

    @Override
    public void start()
    {   entity.setAttackPhase(HoveringInfernoEntity.AttackPhase.BLOCKING);
        this.ticksRemaining = 10;
        entity.playSound(SoundEvents.FIRECHARGE_USE, 1, 1);
    }

    @Override
    public void stop()
    {   entity.setAttackPhase(HoveringInfernoEntity.AttackPhase.NONE);
        entity.setRandomAttackCooldown();
        entity.setDeltaMovement(entity.getDeltaMovement().scale(0.2f));
        lastUsedTimestamp = entity.tickCount;
    }

    @Override
    public void tick()
    {
        if (entity.getTarget() != null)
        {
            LivingEntity target = entity.getTarget();
            entity.setDeltaMovement(target.position().subtract(entity.getEyePosition()).normalize().scale(1.2f));

            for (int i = 0; i < 3; i++)
            {
                ReDevPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), new ParticlesMessage(
                        ParticleTypes.FLAME,
                        entity.level,
                        entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(),
                        2, 2, 2,
                        0, 0, 0, 1));
            }

            if (entity.getBoundingBox().intersects(target.getBoundingBox()))
            {
                target.hurt(entity.damageSources().explosion(entity, entity), 8);
                target.knockback(target.isOnGround() ? 4 : 1.5, entity.getX() - target.getX(), entity.getZ() - target.getZ());

                ReDevPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), new ParticlesMessage(
                        ParticleTypes.EXPLOSION_EMITTER,
                        entity.level,
                        entity.getX(), entity.getY(), entity.getZ(),
                        0, 0, 0, 0, 0, 0, 1));
                entity.playSound(SoundEvents.GENERIC_EXPLODE, 1, 1);

                this.stop();
            }
            this.ticksRemaining--;
        }
    }

    @Override
    public boolean requiresUpdateEveryTick()
    {   return true;
    }
}
