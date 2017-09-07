import vorpal from 'vorpal'
import { words } from 'lodash'
import { connect } from 'net'
import { Message } from './Message'

export const cli = vorpal()

let username
let server
let finalHost
let finalPort

cli
  .delimiter(cli.chalk['yellow']('ftd~$'))

cli
  .mode('connect <username> [host] [port]', 'Type connect followed by your username')
  .delimiter(cli.chalk['green']('connected>'))
  .init(function (args, callback) {

    // args.host !== null ? finalHost = args.host : finalHost = 'localhost'
    // args.port !== null ? finalPort = args.port : finalPort = '8080'

    username = args.username
    server = connect({ host: 'localhost', port: 8080 }, () => {
      server.write(new Message({ username, command: 'connect' }).toJSON() + '\n')
      callback()
    })

    server.on('data', (buffer) => {
    //   if (Message.fromJSON(buffer).command === 'connect') {
    //     this.log(chalk.red(Message.fromJSON(buffer).toString()))
    //   } else if (Message.fromJSON(buffer).command === 'disconnect') {
    //     this.log(chalk.white(Message.fromJSON(buffer).toString()))
    //   } else if (Message.fromJSON(buffer).command === 'echo') {
    //     this.log(chalk.blue(Message.fromJSON(buffer).toString()))
    //   } else if (Message.fromJSON(buffer).command === 'broadcast') {
    //     this.log(chalk.magenta(Message.fromJSON(buffer).toString()))
    //   }  else if (Message.fromJSON(buffer).command === 'users') {
    //     this.log(chalk.cyan(Message.fromJSON(buffer).toString()))
    //   } else if (Message.fromJSON(buffer).command.startsWith("@")) {
    //     this.log(chalk.yellow(Message.fromJSON(buffer).toString()))
    //  }  else if (Message.fromJSON(buffer).command===null) {
    //    this.log(chalk.grey(Message.fromJSON(buffer).toString()))
    // } else {
      this.log(Message.fromJSON(buffer).toString())
    
    })

    server.on('end', () => {
      cli.exec('exit')
    })
  })
  .action(function (input, callback) {

    const [ command, ...rest ] = words(input, /\S+/g)
    const contents = rest.join(' ')


    // Add condition to allow in null values
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
