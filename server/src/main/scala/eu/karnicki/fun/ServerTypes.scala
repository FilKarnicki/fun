package eu.karnicki.fun

type TradeId = String
type TraderId = String
type ContractId = String
type CounterpartyHash = String
type CounterpartyId = String
type InstrumentId = String

enum Status:
  case live, dead

enum TradeType:
  case forward, option

case class Event(traderId: TraderId, notional: BigDecimal, anonymizedBuyer: CounterpartyHash, anonymizedSeller: CounterpartyHash)

case class EnrichedEvent(event: Event, buyer: CounterpartyId, seller: CounterpartyId)

case class Trader(id: TraderId, name: String)

case class Instrument(id: InstrumentId, notional: BigDecimal)

case class Contract(id: ContractId, obligations: Seq[Obligation])

case class Obligation(buyer: CounterpartyId, seller: CounterpartyId, instrument: Instrument, status: Status)