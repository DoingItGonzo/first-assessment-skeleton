import vorpal from 'vorpal'
import { words } from 'lodash'
import { connect } from 'net'
import { Message } from './Message'

export const cli = vorpal()

let username
let server

cli
  .delimiter(cli.chalk['yellow']('ftd~$'))

cli
  .mode('connect <username> [host] [port]')
  .delimiter(cli.chalk['green']('connected>'))
  .init(function (args, callback) {

    let host = 'localhost'
    let port = 8080
    if (args.port) port = args.port
    if (args.host) host = args.host
     
    username = args.username
    server = connect({ host, port }, () => {
      server.write(new Message({ username, command: 'connect' }).toJSON() + '\n')
      callback()
    })

    server.on('data', (buffer) => {
      let message = Message.fromJSON(buffer)

      let commandColorMap = {'connect': 'red', 'disconnect': 'white',
      'echo': 'blue', 'broadcast': 'magenta', 'users': 'cyan', null: 'grey' } 

      if (message.command.startsWith("@")) {
        this.log(cli.chalk['yellow'](message.toString()))
      } else {
        this.log(cli.chalk[commandColorMap[message.command]](message.toString()))
      } 
    })

    server.on('end', () => {
      cli.exec('exit')
    })
  })
  .action(function (input, callback) {

    const [ command, ...rest ] = words(input, /\S+/g)
    const contents = rest.join(' ')


    // Add condition to allow in null values for previous command stuff
    if (command === 'disconnect') {
      server.end(new Message({ username, command }).toJSON() + '\n')
    } else if (command === 'echo') {
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
    } else if (command === 'broadcast') {
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
    } else if (command === 'users') {
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
    } else if (command.startsWith("@")) {
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
    } else {
      this.log(`Command <${command}> was not recognized`)
    }

    callback()
  })
