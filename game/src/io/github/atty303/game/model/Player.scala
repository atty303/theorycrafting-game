package io.github.atty303.game.model

import indigo._
import indigoextras.datatypes.DecreaseTo
import indigo.shared.events.GlobalEvent
import io.github.atty303.game.Pulse
import indigo.shared.materials.Material.Bitmap

final case class Player(saa: Map[CycleLabel, Clip[Bitmap]], skill: DefaultAttack) {
  def update(gameTime: GameTime, timeDelta: Seconds): GlobalEvent => Outcome[Player] = { case e =>
    for {
      skill <- skill.update(gameTime, timeDelta)(e)
    } yield this.copy(skill = skill)
  }
}

object Player {
  def initial(playerSprite: Map[CycleLabel, Clip[Bitmap]]): Player = {
    val a = playerSprite.get(CycleLabel("Attack")).map { clip =>
      clip.sheet.frameCount.toDouble * clip.sheet.frameDuration.toDouble
    }
    Player(playerSprite, DefaultAttack.initial(a.map(Seconds(_)).get))
  }
}

object PlayerView {
  def draw(player: Player, skillActivated: Pulse): Outcome[SceneUpdateFragment] =
    DefaultAttackView.draw(player, skillActivated)
}

final case class DefaultAttack(
    actionTime: Seconds,
    cooldown: Cooldown,
    actionCooldown: Cooldown,
    actionAt: GameTime
) {
  def update(gameTime: GameTime, timeDelta: Seconds): GlobalEvent => Outcome[DefaultAttack] = {
    case DefaultAttack.Events.Ready =>
      Outcome(
        this.copy(
          actionCooldown = Cooldown(actionTime, DefaultAttack.Events.Done).reset(),
          actionAt = gameTime
        )
      )
    case DefaultAttack.Events.Done  =>
      Outcome(this.copy(cooldown = cooldown.reset()))
    case e @ FrameTick              =>
      for {
        cd  <- cooldown.update(timeDelta)(e)
        acd <- actionCooldown.update(timeDelta)(e)
      } yield this.copy(cooldown = cd, actionCooldown = acd)
    case _                          =>
      Outcome(this)
  }
}

object DefaultAttack {
  def initial(actionTime: Seconds): DefaultAttack = DefaultAttack(
    actionTime,
    Cooldown(Seconds(3), Events.Ready).reset(),
    Cooldown(Seconds(0), Events.Done),
    GameTime.zero
  )

  enum Events extends GlobalEvent {
    case Ready, Done
  }
}

object DefaultAttackView:
  def draw(player: Player, b: Pulse): Outcome[SceneUpdateFragment] =
    Outcome(
      SceneUpdateFragment(
        if (player.skill.actionCooldown.isActive)
          player.saa(CycleLabel("Attack")).playOnce(player.skill.actionAt.running)
        else
          player.saa(CycleLabel("Idle"))
      )
    )

final case class Cooldown private (
    init: Seconds,
    readyEvent: GlobalEvent,
    private val dec: Option[DecreaseTo]
) {
  def update(timeDelta: Seconds): GlobalEvent => Outcome[Cooldown] =
    case FrameTick =>
      val (newDec, edge) = dec
        .map { d =>
          val e = d.update(timeDelta)
          if (e.value == 0) None -> true else Some(e) -> false
        }
        .getOrElse(None -> false)
      Outcome(this.copy(dec = newDec))
        .createGlobalEvents(_ => if (edge) List(readyEvent) else Nil)

  def reset(): Cooldown =
    Cooldown(init, readyEvent, Some(DecreaseTo(init.toDouble, 1, 0)))

  def isActive: Boolean = dec.nonEmpty
  def isIdle: Boolean   = !isActive
}

object Cooldown:
  case object Ready extends GlobalEvent

  def apply(init: Seconds, event: GlobalEvent): Cooldown = Cooldown(init, event, None)
