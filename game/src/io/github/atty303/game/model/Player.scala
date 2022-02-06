package io.github.atty303.game.model

import indigo.*
import indigoextras.datatypes.DecreaseTo
import indigo.shared.events.GlobalEvent

final case class Player(saa: SpriteAndAnimations, skill: DefaultAttack) {
  IndigoLogger.debugOnce(pprint(saa).plainText)
  def update(dt: Seconds): GlobalEvent => Outcome[Player] =
    case e =>
      for {
        skill <- skill.update(dt)(e)
      } yield this.copy(skill = skill)
}

object Player:
  def initial(playerSprite: SpriteAndAnimations): Player =
    val a = playerSprite.animations.cycles.find(_.label == CycleLabel("Attack")).map { cycle =>
      cycle.lastFrameAdvance + cycle.frames.map(_.duration).toList.reduce(_ + _)
    }
    Player(playerSprite, DefaultAttack.initial(a.map(_.toSeconds).get))

object PlayerView:
  def draw(player: Player): Outcome[SceneUpdateFragment] =
    DefaultAttackView.draw(player)

final case class DefaultAttack(actionTime: Seconds, cooldown: Cooldown, actionCooldown: Cooldown) {
  def update(timeDelta: Seconds): GlobalEvent => Outcome[DefaultAttack] =
    case DefaultAttack.Events.Ready =>
      IndigoLogger.debug("default attack is ready")
      Outcome(this.copy(actionCooldown = Cooldown(actionTime, DefaultAttack.Events.Done).reset()))
    case DefaultAttack.Events.Done  =>
      IndigoLogger.debug("default attack is done")
      Outcome(this.copy(cooldown = cooldown.reset()))
    case e @ FrameTick              =>
      for {
        cd  <- cooldown.update(timeDelta)(e)
        acd <- actionCooldown.update(timeDelta)(e)
      } yield this.copy(cooldown = cd, actionCooldown = acd)
    case _                          =>
      Outcome(this)
}

object DefaultAttack:
  def initial(actionTime: Seconds): DefaultAttack = DefaultAttack(
    actionTime,
    Cooldown(Seconds(3), Events.Ready).reset(),
    Cooldown(Seconds(0), Events.Done)
  )

  enum Events extends GlobalEvent:
    case Ready, Done

object DefaultAttackView:
  def draw(player: Player): Outcome[SceneUpdateFragment] =
    Outcome(
      SceneUpdateFragment(
        if (player.skill.actionCooldown.isActive)
          player.saa.sprite.changeCycle(CycleLabel("Attack")).play()
        else
          player.saa.sprite.changeCycle(CycleLabel("Idle")).play()
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
