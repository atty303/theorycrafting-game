package io.github.atty303.game.model

import indigo.*
import indigoextras.datatypes.DecreaseTo
import indigo.shared.events.GlobalEvent

final case class Player(saa: SpriteAndAnimations, skill: DefaultAttack) {
  def update(dt: Seconds): GlobalEvent => Outcome[Player] =
    case e =>
      for {
        skill <- skill.update(dt)(e)
      } yield this.copy(skill = skill)
}

final case class DefaultAttack(cooldown: Cooldown) {
  def update(timeDelta: Seconds): GlobalEvent => Outcome[DefaultAttack] =
    case Cooldown.Ready =>
      for {
        cd <- cooldown.reset()
        _ = IndigoLogger.debug("default attack ready")
      } yield this.copy(cooldown = cd)
    case _              =>
      for {
        cd <- cooldown.update(timeDelta)
      } yield this.copy(cooldown = cd)
}

object DefaultAttack {
  def init: DefaultAttack = DefaultAttack(Cooldown(Seconds(3)))
}

final case class Cooldown(init: Seconds, dec: DecreaseTo) {
  def update(timeDelta: Seconds): Outcome[Cooldown] =
    Outcome(this.copy(dec = dec.update(timeDelta))).createGlobalEvents { f =>
      if (f.dec.value == 0) List(Cooldown.Ready) else Nil
    }

  def reset(): Outcome[Cooldown] = Outcome(Cooldown(init))
}

object Cooldown {
  case object Ready extends GlobalEvent

  def apply(init: Seconds): Cooldown = Cooldown(init, DecreaseTo(init.toDouble, 1, 0))
}
