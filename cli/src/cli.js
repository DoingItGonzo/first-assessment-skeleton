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

      //
      //condense into one function that calls a map w/ command as key
      // and color as value, son
      //
      if (message.command === 'connect') {
        this.log(cli.chalk['red'](message.toString()))
      } else if (message.command === 'disconnect') {
        this.log(cli.chalk['white'](message.toString()))
      } else if (message.command === 'echo') {
        this.log(cli.chalk['blue'](message.toString()))
      } else if (message.command === 'broadcast') {
        this.log(cli.chalk['magenta'](message.toString()))
      }  else if (message.command === 'users') {
        this.log(cli.chalk['cyan'](message.toString()))
      } else if (message.command.startsWith("@")) {
        this.log(cli.chalk['yellow'](message.toString()))
     }  else if (message.command===null) {
       this.log(cli.chalk['grey'](message.toString()))
    } else {
      this.log(message.toString())
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
