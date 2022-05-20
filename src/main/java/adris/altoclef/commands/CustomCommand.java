package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.Settings;
import adris.altoclef.commandsystem.Arg;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;

public class CustomCommand extends Command {
    //TODO Delete dummyAlto and party after settings is updated to include the
    //  custom command json
    static class dummyAlto extends AltoClef{
        /**
         * AltoClef Settings [stub]
         */

       static class dummySettings extends Settings {
            public static String getCustomPrefix(){
                return "custom";
            }
        }
    }

    public CustomCommand() throws CommandException{
        /*I setup this structure here so we could recognize that the setting
        * will have to be pulled from mod. This means we will probably need
        * to add mode as a constructor parameter, or as an interface.
        *
        * if we keep the constructor default, then altoclef must be static.
        * Alternatievly, we can just not allow custom prefix to be declared. */
        super(dummyAlto.dummySettings.getCustomPrefix(),"Execute a custom command",new Arg(String.class,"custom command"));

        //TODO use the prefix defined in the CustomTask.json file

    }
    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        /*TODO if parser returns a string, compare it to the json file.
            If the string matches a custom command, execute that command.

        */
        String command = parser.get(String.class);
        if(command.equals("gearup")){
            run(mod,command,null); //TODO determine the best way to do this.
        }

    }
}
