/**
 *
 *  Thermostat app. Hot and Cold.
 *  Copyright 2020
 *  Johannes Kmieciak
 */
definition(
    name: "Hot and Cold",
    namespace: "hotandcold",
    author: "Johannes Kmieciak",
    description: "Monitors temperature and turns on heaters when it drops below the target-temperature and switches them of once it is reached.",
    category: "Convenience",
    iconUrl: "https://raw.githubusercontent.com/jokmie/tibber-integration/master/TibberJKIcon.png",
    iconX2Url: "https://raw.githubusercontent.com/jokmie/tibber-integration/master/TibberJKIcon.png",
    pausable: true
)

preferences {
	section("Choose the measuring point:") {
		input "temperatureSensor1", "capability.temperatureMeasurement", required: true
	}
	section("Choose the target-temperature:") {
		input "temperature1", "number", title: "Target-temperature", required: true
	}
	section("Choose the switch to turn on:") {
		input "switch1", "capability.switch", required: true
	}
    section("Interval for the temperature to be checked (in minutes):") {
		input "interval", "number", title: "Measurement interval", required: true
	}
    section( "Notifications:" ) {
        input("recipients", "contact", title: "Send notifications to") {
            input "sendPushMessage", "enum", title: "Send a push notification?", options: ["Yes", "No"], required: true
        }
    }
}

def installed() {
	subscribe(temperatureSensor1, "temperature", temperatureHandler)
}

def updated() {
	unsubscribe()
	subscribe(temperatureSensor1, "temperature", temperatureHandler)
}

def temperatureHandler(evt) {
	log.trace "temperature: $evt.value, $evt"

	def targettemp = temperature1
	def targetswitch = settings.switch1
    def retrievalinterval = settings.interval

	if (evt.doubleValue <= targettemp) {
		def timeAgo = new Date(now() - (1000 * 60 * retrievalinterval).toLong())
		def recentEvents = temperatureSensor1.eventsSince(timeAgo)?.findAll { it.name == "temperature" }
		log.trace "Found ${recentEvents?.size() ?: 0} events in the last $retrievalinterval minutes"
        log.debug "Temperature dropped below $targettemp: sending notification and switching on $targetswitch"
        def tempScale = location.temperatureScale ?: "C"
        send("${temperatureSensor1.displayName} is too cold ${evt.value}${evt.unit?:tempScale}, switching on ${switch1.displayName}")
        switch1?.on()
    }
    if (evt.doubleValue > targettemp) {
		def timeAgo = new Date(now() - (1000 * 60 * retrievalinterval).toLong())
		def recentEvents = temperatureSensor1.eventsSince(timeAgo)?.findAll { it.name == "temperature" }
		log.trace "Found ${recentEvents?.size() ?: 0} events in the last $retrievalinterval minutes"
        log.debug "Temperature rose above $targettemp: sending notification and switching off $targetswitch"
        def tempScale = location.temperatureScale ?: "C"
        send("${temperatureSensor1.displayName} is too warm ${evt.value}${evt.unit?:tempScale}, switching off ${switch1.displayName}")
        switch1?.off()
    }
}

private send(msg) {
    if (sendPushMessage != "No") {
        log.debug("sending push message")
        sendPush(msg)
    }
log.debug msg
}