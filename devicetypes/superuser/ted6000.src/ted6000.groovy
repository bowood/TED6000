preferences {
	input("deviceIP", "string", title:"IP Address", description: "IP Address", required: true, displayDuringSetup: true)
}

metadata {
	// Automatically generated. Make future change here.
    // Original ideas from "swap-file" found in smartthings forum
	definition (name: "TED6000", author: "bowood") {
		capability "Energy Meter"
		capability "Polling"
		capability "Refresh"
		capability "Sensor"
		capability "Power Meter"
	}

	// UI tile definitions
	tiles (scale: 2){
		
		valueTile(	"power", "device.power", width: 4, height:4) 		{
			state(	"device.power",label:'${currentValue} W', backgroundColors:[
			[value: 200, color: "#153591"],
			[value: 400, color: "#1e9cbb"],
			[value: 600, color: "#90d2a7"],
			[value: 700, color: "#44b621"],
			[value: 1000, color: "#f1d801"],
			[value: 1200, color: "#d04e00"],
			[value: 1400, color: "#bc2323"]
			]
			)
		}
		
		valueTile(	"voltage", "device.voltage",width: 2,  height: 1 ) {
			state("device.voltage", label:'${currentValue} Volts' )
		}
		
        	
		valueTile(	"power_factor", "device.power_factor",width: 2,  height: 1 ) {
			state("device.power_factor", label:'PF: ${currentValue}%' )
		}
        
		valueTile("refresh", "command.refresh",width: 2,  height: 2) {
			state "default", label:'refresh', action:"refresh.refresh", icon:"st.secondary.refresh-icon"
		}
		
		//valueTile(	"daily_max_power","device.daily_max_power",width: 4,height: 1) {
		//	state("device.daily_max_power", label:'Daily Max: ${currentValue} W')
		//}
		//	valueTile(	"daily_total_power","device.daily_total_power",width: 4,height: 1) {
		//	state("device.daily_total_power", label:'Total: ${currentValue} kWh')
		//}
		//valueTile(	"daily_min_power","device.daily_min_power",width: 4,height: 1) {
		//	state("device.daily_min_power",label:'Daily Min: ${currentValue} w')
		//}
		
		//valueTile(	"cost", "device.cost",width: 2,  height: 1) {
		//	state("device.cost", label:'\044${currentValue} CPH')
		//}
		main(["power"])
        
		//details(["power","daily_max_power","daily_min_power","daily_total_power","refresh", "cost","voltage","power_factor"])
		details(["power","refresh","voltage","power_factor"])
	}
}

def poll() {
	log.trace 'Poll Called'
	runCmd()
}

def refresh() {
	log.trace 'Refresh Called'
	runCmd()
}

def runCmd() {
	def host = deviceIP
	def hosthex = convertIPtoHex(host).toUpperCase()
	def LocalDevicePort = "80"
	def porthex = convertPortToHex(LocalDevicePort).toUpperCase()
	device.deviceNetworkId = "$hosthex:$porthex"

	//log.debug "The device id configured is: $device.deviceNetworkId"

	def headers = [:] 
	headers.put("HOST", "$host:$LocalDevicePort")
	headers.put("Content-Type", "application/x-www-form-urlencoded")
	
	//log.debug "The Header is $headers"
	
	//def path = '/api/LiveData.xml'
    def path = '/api/SystemOverview.xml'
	def body = ''
	//log.debug "Uses which method: $DevicePostGet"
	def method = "GET"

	try {
		log.debug "Making TED5000 request to $device.deviceNetworkId"
		def hubAction = new physicalgraph.device.HubAction(
method: method,
path: path,
body: body,
headers: headers
		)
		hubAction.options = [outputMsgToS3:false]
		//log.debug hubAction
		hubAction
	}
	catch (Exception e) {
		log.debug "Hit Exception $e on $hubAction"
	}
}

private String convertIPtoHex(ipAddress) { 
	String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
	//log.debug "IP address entered is $ipAddress and the converted hex code is $hex"
	return hex
}
private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04x', port.toInteger() )
	//log.debug hexport
	return hexport
}

def parse(String description) {
	//this is automatically called when the hub action returns
	def msg = parseLanMessage(description).body
	//log.debug "Got Reply: $msg"
    def xml = new XmlSlurper().parseText(msg)
    
	def evt1 = createEvent (name: "power", value: (xml.MTUVal.MTU1.Value).toInteger(), unit:"W")
    def evt2 = createEvent (name: "voltage", value: ((xml.MTUVal.MTU1.Voltage).toDouble() / 10.0), unit:"V")
	def evt3 = createEvent (name: "power_factor", value: ((xml.MTUVal.MTU1.PF).toDouble() / 10.0), unit:"%")
	return [evt1, evt2,evt3]
}