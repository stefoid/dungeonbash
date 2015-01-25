#!/usr/bin/env python
#!/usr/bin/env python
#Boa:PyApp:main

#modules ={}

#def main():
#    pass

#if __name__ == '__main__':
#    main()

import string
import types
from Tkinter import *
import Tkinter
#import ImageTk
#import Image

# globals
parsingAbilities = 1
abilityIndex = 0
creatureIndex = 0
ability = []
creature = []
lookingAtAbilities = 1
currentAbility = 0
currentCreature = 0
kindOfNewAbility = 1
resString = ""
searchString = ""

im = ""
# end of globals


#classes

class Command:
    def __init__(self):     
        self.name = 0
        self.execute = 0
        self.param1 = 0
        self.param2 = 0
        self.param3 = 0
        self.param4 = 0  

class Ability:
    def __init__(self): 
        self.damageStr = [0,0,0,0,0,0]
        self.valsetStr = [0,0,0,0,0,0]
        self.valmodStr = [0,0,0,0,0,0]
        self.valmulStr = [0,0,0,0,0,0]
        self.value = 1;
        self.offensive = 1;
        self.cooldown = 0;
    
    def fileWrite(self, file):
        self.comment = "// "+self.name + ": "
        st = "+\"" + self.name + "," + self.gifname + ","+str(self.colour)+"," + str(self.needs) +"," + str(self.invokingStrategy)+"," +str(self.duration) +"," +str(self.physicalItem) +"," + str(self.magicCost)+"," + str(self.aimed)+"," + str(self.value)+"," + str(self.offensive)+"," + str(self.cooldown)+"," 
        for i in range (0,6):
            if (self.commands[i].name > 0):
                st = st + str(self.commands[i].name) + "," + str(self.commands[i].execute)+ "," +str(self.commands[i].param1) + "," +str(self.commands[i].param2) + "," +str(self.commands[i].param3) + "," +str(self.commands[i].param4) + ","
                self.comment = self.comment + convertStrategy(self.commands[i].execute) + ","
        st = st + "*\"" + self.comment + "\n"
        st = string.replace(st,"_", " ")
        file.write(st)

    def fileWrite2(self, file):
        st = "" + self.name + "," + self.gifname + ","+str(self.colour)+"," + str(self.needs) +"," + str(self.invokingStrategy)+"," +str(self.duration) +"," +str(self.physicalItem) +"," + str(self.magicCost)+"," + str(self.aimed)+"," + str(self.value)+"," + str(self.offensive)+","+ str(self.cooldown)+","
        for i in range (0,6):
            if (self.commands[i].name > 0):
                st = st + str(self.commands[i].name) + "," + str(self.commands[i].execute)+ "," +str(self.commands[i].param1) + "," +str(self.commands[i].param2) + "," +str(self.commands[i].param3) + "," +str(self.commands[i].param4) + ","
        st = st + "*"
        st = string.replace(st,"_", " ")
        file.write(st)

    def convert(self):  # will convert between numerical and string values and back again
        self.colour = convertColour(self.colour)
        self.needs = convertNeeds(self.needs)
        self.invokingStrategy = convertInvoke(self.invokingStrategy)
        self.duration = convertDur(self.duration)
        for com in self.commands:
            com.name = convertExecute(com.name)
            if com.name == "RESIST_ABILITY" or com.name == 21:
                com.param1 = convertAbilityId(com.param1)

            com.execute = convertStrategy(com.execute)
            if com.execute == "ATTACKER" or com.execute == 3:
                com.param1 = convertAttack(com.param1)
                com.param3 = convertAbilityId(com.param3)
                com.param4 = convertRange(com.param4)
            elif com.execute == "ABILITY_ADDER" or com.execute == 4:
                com.param1 = convertAbilityId(com.param1)
            elif com.execute == "SELECT" or com.execute == 6:
                com.param1 = convertClear(com.param1)    
                com.param2 = convertAbilityId(com.param2)              

    def update(self):
        self.name = self.nameStr.get()
        self.gifname = self.gifnameStr.get()
        self.physicalItem = self.physicalItemStr.get()
        self.magicCost = self.magicCostStr.get()
        self.aimed = self.aimedStr.get()
        self.value = self.valueStr.get()
        self.offensive = self.offStr.get()
        self.cooldown = self.coolStr.get()
        
        for i in range(0,6):
            if self.commands[i].execute == "ATTACKER":
                self.commands[i].param2 = self.damageStr[i].get()
            elif self.commands[i].execute == "VALUE_SETTER":
                self.commands[i].param1 = self.valsetStr[i].get()
            elif self.commands[i].execute == "VALUE_MODIFIER":
                self.commands[i].param1 = self.valmodStr[i].get()
            elif self.commands[i].execute == "VALUE_MULTIPLIER":
                self.commands[i].param1 = self.valmulStr[i].get()
                
    def draw(self, idnum):
        l1 = Label(win, text="       id         ")
        l1.grid(row=0, column=0)        
        id = Label(win, text=idnum)
        id.grid(row=1, column=0)
                
        l2 = Label(win, text="name              ")
        l2.grid(row=0, column=1)
        self.nameStr = StringVar()
        self.nameStr.set(self.name)
        te = Entry(win, textvariable=self.nameStr)
        te.grid(row=1, column=1)

        l21 = Label(win, text="gifname            ")
        l21.grid(row=0, column=2)        
        self.gifnameStr = StringVar()
        self.gifnameStr.set(self.gifname)
        tg = Entry(win, textvariable=self.gifnameStr) 
        tg.grid(row=1, column=2)
    
        l333 = Label(win, text="picture       ")
        l333.grid(row=0, column=3)
        can = Canvas(win, width=20, height = 20, bg="white")
        can.grid(row=1, column=3)
        try:
            global im
            im = Tkinter.PhotoImage(file=resString + self.gifname + ".gif")
            imag = can.create_image(10,10,image=im)
        except:
            bit = can.create_bitmap(10,10,bitmap="error")

        l3 = Label(win, text="    colour        ")
        l3.grid(row=0, column=4)
        com = Menubutton( win, text=self.colour, relief=RAISED)
        com.grid(row=1, column=4)        
        com.menu = Menu(com, tearoff=0)
        com["menu"] = com.menu
        for i in ["TRANSPARENT","BLACK","BLUE","LIME","AQUA","RED","FUCHSIA","YELLOW","WHITE","GRAY","NAVY","GREEN","TEAL","MAROON","PURPLE","OLIVE","SILVER"]:
            com.menu.add_command(label=i, command=lambda i=i: setAbility("colour",i,0))
        
        l4 = Label(win, text="        needs     ")
        l4.grid(row=0, column=5)
        com1 = Menubutton( win, text=self.needs, relief=RAISED)
        com1.grid(row=1, column=5)        
        com1.menu = Menu(com1, tearoff=0)
        com1["menu"] = com1.menu
        for i in ["NONE","HEAD","HANDS","HUMANOID"]:
            com1.menu.add_command(label=i, command=lambda i=i: setAbility("needs",i,0))
                            
        l5 = Label(win, text="    strategy      ")
        l5.grid(row=0, column=6)
        com2 = Menubutton( win, text=self.invokingStrategy, relief=RAISED)
        com2.grid(row=1, column=6)        
        com2.menu = Menu(com2, tearoff=0)
        com2["menu"] = com2.menu
        for i in ["NOT_SELECTABLE","INSTANT_ABILITY","TARGETABLE_ABILITY","SELECTABLE", "INSTANT_ONESHOT"]:
            com2.menu.add_command(label=i, command=lambda i=i: setAbility("strategy",i,0))

        l6 = Label(win, text="duration          ")
        l6.grid(row=0, column=7)
        com3 = Menubutton( win, text=self.duration, relief=RAISED)
        com3.grid(row=1, column=7)        
        com3.menu = Menu(com3, tearoff=0)
        com3["menu"] = com3.menu
        for i in ["PERMANENT",1,2,3,4,5,8,10,15,20,30,40,50,70,100]:
            com3.menu.add_command(label=i, command=lambda i=i: setAbility("duration",i,0))
                    
        l7 = Label(win, text="physical?         ")
        l7.grid(row=3, column=7)
        self.physicalItemStr = StringVar()
        self.physicalItemStr.set(self.physicalItem)
        tg2 = Entry(win, textvariable=self.physicalItemStr)
        tg2.grid(row=4, column=7)
        
        l8 = Label(win, text="magic cost        ")
        l8.grid(row=5, column=7)
        self.magicCostStr = StringVar()
        self.magicCostStr.set(self.magicCost)
        tg3 = Entry(win, textvariable=self.magicCostStr)
        tg3.grid(row=6, column=7)
        
        l9 = Label(win, text="aimed?            ")
        l9.grid(row=7, column=7)
        self.aimedStr = StringVar()
        self.aimedStr.set(self.aimed)
        tg4 = Entry(win, textvariable=self.aimedStr)
        tg4.grid(row=8, column=7)

        l91 = Label(win, text="value        ")
        l91.grid(row=9, column=7)
        self.valueStr = StringVar()
        self.valueStr.set(self.value)
        tg41 = Entry(win, textvariable=self.valueStr)
        tg41.grid(row=10, column=7)
        
        l91 = Label(win, text="offensive   ")
        l91.grid(row=11, column=7)
        self.offStr = StringVar()
        self.offStr.set(self.offensive)
        tg41 = Entry(win, textvariable=self.offStr)
        tg41.grid(row=12, column=7)
        
        l91 = Label(win, text="cooldown   ")
        l91.grid(row=13, column=7)
        self.coolStr = StringVar()
        self.coolStr.set(self.cooldown)
        tg41 = Entry(win, textvariable=self.coolStr)
        tg41.grid(row=14, column=7)
        
        # now do up to 6 commands that this ability may or may not respond to
        cRow=2
        for i in range(0,6):
            ll1 = Label(win, text="command name    ")
            ll1.grid(row=cRow, column=1)
            cm1 = Menubutton( win, text=self.commands[i].name, relief=RAISED)
            cm1.grid(row=cRow+1, column=1)        
            cm1.menu = Menu(cm1, tearoff=0)
            cm1["menu"] = cm1.menu
            for j in [0,"RESIST_HARD","RESIST_SHARP","RESIST_ENERGY","RESIST_CHEMICAL", "CLEAR", "CLEAR_ARMOR",
                "CLEAR_AMULET","CLEAR_MELEE","MELEE_ATTACK","SET","MODIFY_SPEED","MODIFY_HEALTH","MODIFY_MAX_HEALTH","MODIFY_MAGIC",
                "MODIFY_MAX_MAGIC","MODIFY_ATTACK_SKILL","MODIFY_DEFENCE_SKILL","INVOKE","EXECUTE","CANCEL","RESIST_ABILITY",
                "MODIFY_STEALTH", "MODIFY_DETECT", "MODIFY_MISSILE_DEFENCE", "RESIST_BURST"]:
                cm1.menu.add_command(label=j, command=lambda i=i,j=j: setAbility("comname",j,i))            
            
            if self.commands[i].name == "RESIST_ABILITY":
                ll5 = Label(win, text="ability to resist")
                ll5.grid(row=cRow, column=3)
                cm4 = Menubutton( win, text=self.commands[i].param1, relief=RAISED)
                cm4.grid(row=cRow+1, column=3)        
                cm4.menu = Menu(cm4, tearoff=0)
                cm4["menu"] = cm4.menu
                cm4.menu.add_command(label="NO_ABILITY", command=lambda i=i,j=j: setAbility("par1","NO_ABILITY",i))  
                for j in ability:
                    cm4.menu.add_command(label=j.name, command=lambda i=i,j=j: setAbility("par1",j.name,i)) 
            else:
                ll2 = Label(win, text="exec strategy   ")
                ll2.grid(row=cRow, column=2)   
                cm2 = Menubutton( win, text=self.commands[i].execute, relief=RAISED)
                cm2.grid(row=cRow+1, column=2)        
                cm2.menu = Menu(cm2, tearoff=0)
                cm2["menu"] = cm2.menu
                for j in ["NO_STRATEGY","VALUE_MODIFIER","VALUE_SETTER","VALUE_MULTIPLIER", "ATTACKER", "ABILITY_ADDER", "DESELECT","SELECT"]:
                    cm2.menu.add_command(label=j, command=lambda i=i,j=j: setAbility("exec",j,i ))                   
    
                if self.commands[i].execute == "ATTACKER": 
                    ll3 = Label(win, text="attack type ")
                    ll3.grid(row=cRow, column=3)
                    cm3 = Menubutton( win, text=self.commands[i].param1, relief=RAISED)
                    cm3.grid(row=cRow+1, column=3)        
                    cm3.menu = Menu(cm3, tearoff=0)
                    cm3["menu"] = cm3.menu
                    for j in ["NONE", "HARD","SHARP","ENERGY", "CHEMICAL", "KNOCKBACK"]:
                        cm3.menu.add_command(label=j, command=lambda i=i,j=j: setAbility("par1",j,i))  
                        
                    ll4 = Label(win, text="damage    ")
                    ll4.grid(row=cRow, column=4)
                    self.damageStr[i] = StringVar()
                    self.damageStr[i].set(self.commands[i].param2)
                    tg5 = Entry(win, textvariable=self.damageStr[i])
                    tg5.grid(row=cRow+1, column=4)    
                                         
                    ll5 = Label(win, text="ability to add")
                    ll5.grid(row=cRow, column=5)
                    cm4 = Menubutton( win, text=self.commands[i].param3, relief=RAISED)
                    cm4.grid(row=cRow+1, column=5)        
                    cm4.menu = Menu(cm4, tearoff=0)
                    cm4["menu"] = cm4.menu
                    cm4.menu.add_command(label="NO_ABILITY", command=lambda i=i,j=j: setAbility("par3","NO_ABILITY",i))  
                    for j in ability:
                        cm4.menu.add_command(label=j.name, command=lambda i=i,j=j: setAbility("par3",j.name,i)) 
                                        
                    ll6 = Label(win, text="range          ")
                    ll6.grid(row=cRow, column=6)
                    cm5 = Menubutton( win, text=self.commands[i].param4, relief=RAISED)
                    cm5.grid(row=cRow+1, column=6)        
                    cm5.menu = Menu(cm5, tearoff=0)
                    cm5["menu"] = cm5.menu 
                    for j in [0,"LOS",1,2,3,4,5,6,7,8,9,10,15]:
                        cm5.menu.add_command(label=j, command=lambda i=i,j=j: setAbility("par4",j,i))                 
                       
                elif self.commands[i].execute == "SELECT":
                    ll3 = Label(win, text="type to clear   ")
                    ll3.grid(row=cRow, column=3)
                    cm3 = Menubutton( win, text=self.commands[i].param1, relief=RAISED)
                    cm3.grid(row=cRow+1, column=3)        
                    cm3.menu = Menu(cm3, tearoff=0)
                    cm3["menu"] = cm3.menu
                    for j in ["CLEAR_NONE","CLEAR_ARMOUR", "CLEAR_AMULET", "CLEAR_MELEE"]:
                        cm3.menu.add_command(label=j, command=lambda i=i,j=j: setAbility("par1",j,i)) 
                    ll5 = Label(win, text="ability to add")
                    ll5.grid(row=cRow, column=4)
                    cm4 = Menubutton( win, text=self.commands[i].param2, relief=RAISED)
                    cm4.grid(row=cRow+1, column=4)        
                    cm4.menu = Menu(cm4, tearoff=0)
                    cm4["menu"] = cm4.menu
                    cm4.menu.add_command(label="NO_ABILITY", command=lambda i=i,j=j: setAbility("par2","NO_ABILITY",i))  
                    for j in ability:
                        cm4.menu.add_command(label=j.name, command=lambda i=i,j=j: setAbility("par2",j.name,i))                     
                elif self.commands[i].execute == "VALUE_MODIFIER":
                    ll3 = Label(win, text="value mod by   ")
                    ll3.grid(row=cRow, column=3)   
                    self.valmodStr[i] = StringVar()
                    self.valmodStr[i].set(self.commands[i].param1)
                    tg5 = Entry(win, textvariable=self.valmodStr[i])                
                    tg5.grid(row=cRow+1, column=3)                  
                elif self.commands[i].execute == "VALUE_SETTER":
                    ll3 = Label(win, text="value set to   ")
                    ll3.grid(row=cRow, column=3)                 
                    self.valsetStr[i] = StringVar()
                    self.valsetStr[i].set(self.commands[i].param1)
                    tg5 = Entry(win, textvariable=self.valsetStr[i])   
                    tg5.grid(row=cRow+1, column=3)   
                elif self.commands[i].execute == "VALUE_MULTIPLIER":
                    ll3 = Label(win, text="value times %")
                    ll3.grid(row=cRow, column=3)                 
                    self.valmulStr[i] = StringVar()
                    self.valmulStr[i].set(self.commands[i].param1)
                    tg5 = Entry(win, textvariable=self.valmulStr[i])   
                    tg5.grid(row=cRow+1, column=3)              
                elif self.commands[i].execute == "ABILITY_ADDER":
                    ll3 = Label(win, text="ability to add   ")
                    ll3.grid(row=cRow, column=3)                 
                    cm4 = Menubutton( win, text=self.commands[i].param1, relief=RAISED)
                    cm4.grid(row=cRow+1, column=3)        
                    cm4.menu = Menu(cm4, tearoff=0)
                    cm4["menu"] = cm4.menu
                    cm4.menu.add_command(label="NO_ABILITY", command=lambda i=i,j=j: setAbility("par1","NO_ABILITY",i))  
                    for j in ability:
                        cm4.menu.add_command(label=j.name, command=lambda i=i,j=j: setAbility("par1",j.name,i))                                     
             
            cRow = cRow+2
                                                 
class Creature:
    def __init__(self):    
        self.name = "new"
        self.gifname  = "a"
        self.colour = "WHITE"
        self.head = 1
        self.hands = 0
        self.humanoid = 0
        self.swarm = 0
        self.maxHealth = 0
        self.maxMagic = 0
        self.speed = 0
        self.attack = 0
        self.defence = 0  
        self.value = 1
        self.stealth = 1
        self.detect = 1
        self.starter = 0
        self.abilities = ["NO_ABILITY","NO_ABILITY","NO_ABILITY","NO_ABILITY","NO_ABILITY","NO_ABILITY"]    

    def fileWrite(self, file):
        self.comment = "// "+self.name + ": "
        st = "+\"" + self.name + "," + self.gifname + ","+str(self.colour)+"," + str(self.head) +"," + str(self.hands)+"," +str(self.humanoid) +"," +str(self.swarm) +"," + str(self.maxHealth)+"," + str(self.maxMagic)+"," +str(self.speed) +"," + str(self.attack)+"," + str(self.defence)+","+ str(self.value)+"," + str(self.stealth)+"," + str(self.detect)+","+ str(self.starter)+","
        for i in range (0,6):
            if (self.abilities[i] != -1):
                st = st + str(self.abilities[i]) + "," 
                self.comment = "" + self.comment + convertAbilityId(self.abilities[i]) + " , "
        st = st + "*\"" + self.comment + "\n"
        st = string.replace(st,"_", " ")
        file.write(st)

    def fileWrite2(self, file):
        st = "" + self.name + "," + self.gifname + ","+str(self.colour)+"," + str(self.head) +"," + str(self.hands)+"," +str(self.humanoid) +"," +str(self.swarm) +"," + str(self.maxHealth)+"," + str(self.maxMagic)+"," +str(self.speed) +"," + str(self.attack)+"," + str(self.defence)+"," + str(self.value)+","+ str(self.stealth)+"," + str(self.detect)+","+ str(self.starter)+","
        for i in range (0,6):
            if (self.abilities[i] != -1):
                st = st + str(self.abilities[i]) + "," 
                
        st = st + "*"
        st = string.replace(st,"_", " ")
        file.write(st)

    def update(self):
        self.name = self.nameStr.get()
        self.gifname = self.gifnameStr.get()
        self.head = self.headStr.get()
        self.hands = self.handsStr.get()
        self.humanoid = self.humanoidStr.get()
        self.swarm = self.swarmStr.get()
        self.maxHealth = self.maxHealthStr.get()
        self.maxMagic = self.maxMagicStr.get()
        self.speed = self.speedStr.get()
        self.attack = self.attackStr.get()
        self.defence = self.defenceStr.get()
        self.value = self.valueStr.get()
        self.stealth = self.stealthStr.get()
        self.detect = self.detectStr.get()       
        self.starter = self.starterStr.get()       
    
    def convert(self):  # will convert between numerical and string values and back again
        self.colour = convertColour(self.colour)
        i = 0
        for ab in self.abilities:
            self.abilities[i] = convertAbilityId(ab)
            i = i + 1    

    def draw(self, idnum):
        l1 = Label(win, text="       id         ")
        l1.grid(row=0, column=0)        
        id = Label(win, text=idnum)
        id.grid(row=1, column=0)
                
        l2 = Label(win, text="name              ")
        l2.grid(row=0, column=1)
        self.nameStr = StringVar()
        self.nameStr.set(self.name)
        te = Entry(win, textvariable=self.nameStr)
        te.grid(row=1, column=1)

        l21 = Label(win, text="gifname", padx=-15)
        l21.grid(row=0, column=2)        
        self.gifnameStr = StringVar()
        self.gifnameStr.set(self.gifname)
        tg = Entry(win, textvariable=self.gifnameStr)
        tg.grid(row=1, column=2)

        l333 = Label(win, text="picture       ")
        l333.grid(row=0, column=3)
        can = Canvas(win, width=20, height = 20, bg="white")
        can.grid(row=1, column=3)
        try:
            global im
            im = Tkinter.PhotoImage(file=resString + self.gifname + ".gif")
            imag = can.create_image(10,10,image=im)
        except:
            bit = can.create_bitmap(10,10,bitmap="error")

        l3 = Label(win, text=" colour ")
        l3.grid(row=0, column=4)
        com = Menubutton( win, text=self.colour, relief=RAISED)
        com.grid(row=1, column=4)        
        com.menu = Menu(com, tearoff=0)
        com["menu"] = com.menu
        for i in ["TRANSPARENT","BLACK","BLUE","LIME","AQUA","RED","FUCHSIA","YELLOW","WHITE","GRAY","NAVY","GREEN","TEAL","MAROON","PURPLE","OLIVE","SILVER"]:
            com.menu.add_command(label=i, command=lambda i=i: setCreature("colour",i,0))
        
        l4 = Label(win, text="has head")
        l4.grid(row=0, column=5)        
        self.headStr = StringVar()
        self.headStr.set(self.head)
        tg1 = Entry(win, textvariable=self.headStr)
        tg1.grid(row=1, column=5)
                            
        l5 = Label(win, text="has hands")
        l5.grid(row=0, column=6)        
        self.handsStr = StringVar()
        self.handsStr.set(self.hands)
        tg2 = Entry(win, textvariable=self.handsStr)
        tg2.grid(row=1, column=6)

        l26 = Label(win, text="humanoid shape")
        l26.grid(row=0, column=7)        
        self.humanoidStr = StringVar()
        self.humanoidStr.set(self.humanoid)
        tg3 = Entry(win, textvariable=self.humanoidStr)
        tg3.grid(row=1, column=7)
                    
        l7 = Label(win, text="swarm?")
        l7.grid(row=7, column=7)
        self.swarmStr = StringVar()
        self.swarmStr.set(self.swarm)
        tg8 = Entry(win, textvariable=self.swarmStr)
        tg8.grid(row=8, column=7)
        
        l8 = Label(win, text="max health")
        l8.grid(row=2, column=2)
        self.maxHealthStr = StringVar()
        self.maxHealthStr.set(self.maxHealth)
        tg9 = Entry(win, textvariable=self.maxHealthStr)
        tg9.grid(row=3, column=2)

        l9 = Label(win, text="max magic")
        l9.grid(row=2, column=4)
        self.maxMagicStr = StringVar()
        self.maxMagicStr.set(self.maxMagic)
        tgg = Entry(win, textvariable=self.maxMagicStr)
        tgg.grid(row=3, column=4)
        
        l33 = Label(win, text="speed")
        l33.grid(row=2, column=5)
        self.speedStr = StringVar()
        self.speedStr.set(self.speed)
        tgg1 = Entry(win, textvariable=self.speedStr)
        tgg1.grid(row=3, column=5)

        l91 = Label(win, text="attack")
        l91.grid(row=2, column=6)
        self.attackStr = StringVar()
        self.attackStr.set(self.attack)
        tg41 = Entry(win, textvariable=self.attackStr)
        tg41.grid(row=3, column=6)

        l92 = Label(win, text="defence")
        l92.grid(row=2, column=7)
        self.defenceStr = StringVar()
        self.defenceStr.set(self.defence)
        tg42 = Entry(win, textvariable=self.defenceStr)
        tg42.grid(row=3, column=7)

        l93 = Label(win, text="value")
        l93.grid(row=9, column=7)
        self.valueStr = StringVar()
        self.valueStr.set(self.value)
        tg43 = Entry(win, textvariable=self.valueStr)
        tg43.grid(row=10, column=7)   

        l100 = Label(win, text="stealth")
        l100.grid(row=11, column=7)
        self.stealthStr = StringVar()
        self.stealthStr.set(self.stealth)
        tg100 = Entry(win, textvariable=self.stealthStr)
        tg100.grid(row=12, column=7) 

        l101 = Label(win, text="detection")
        l101.grid(row=13, column=7)
        self.detectStr = StringVar()
        self.detectStr.set(self.detect)
        tg101 = Entry(win, textvariable=self.detectStr)
        tg101.grid(row=14, column=7)  

        l102 = Label(win, text="starter")
        l102.grid(row=15, column=7)
        self.starterStr = StringVar()
        self.starterStr.set(self.starter)
        tg102 = Entry(win, textvariable=self.starterStr)
        tg102.grid(row=16, column=7)  
    
        # now do up to 6 abilities that this creatrue may or may not have
        cRow=5
        for i in range(0,6):
            ll1 = Label(win, text="ability")
            ll1.grid(row=cRow, column=1)
            cm4 = Menubutton( win, text=self.abilities[i], relief=RAISED)
            cm4.grid(row=cRow+1, column=1)        
            cm4.menu = Menu(cm4, tearoff=0)
            cm4["menu"] = cm4.menu
            cm4.menu.add_command(label="NO_ABILITY", command=lambda i=i: setCreature("abil","NO_ABILITY",i))  
            cm4.menu.add_command(label="RANDOM_ITEM", command=lambda i=i: setCreature("abil","RANDOM_ITEM",i))  
            cm4.menu.add_command(label="RANDOM_MAGIC", command=lambda i=i: setCreature("abil","RANDOM_MAGIC",i))  
            for j in ability:
                cm4.menu.add_command(label=j.name, command=lambda i=i,j=j: setCreature("abil",j.name,i)) 
            cRow = cRow + 2     
#end of classes

# functions


def setAbility(param, value, index):
    global ability
    global currentAbility
    clearScreen()
    ab = ability[currentAbility]
    if param == "colour":
        ab.colour = value
    elif param == "needs":
        ab.needs = value
    elif param == "strategy":
        ab.invokingStrategy = value
    elif param == "duration":
        ab.duration = value
    elif param == "comname":
        ab.commands[index].name = value
    elif param == "exec":
        ab.commands[index].execute = value
    elif param == "par1":
        ab.commands[index].param1 = value
    elif param == "par2":
        ab.commands[index].param2 = value
    elif param == "par3":
        ab.commands[index].param3 = value
    elif param == "par4":
        ab.commands[index].param4 = value
        
    ab.draw(currentAbility)
    drawMainButtons()

def setCreature(param, value, index):
    global creature
    global currentCreature
    clearScreen()
    ab = creature[currentCreature]
    if param == "colour":
        ab.colour = value
    elif param == "abil":
        ab.abilities[index] = value
        
    ab.draw(currentCreature)
    drawMainButtons()

def convertAbilityId(id):
    count = 0
    result = -1
       
    if type(id) == str:
        for ab in ability:
            if id == ab.name:
                result = count
            count = count + 1
    else:
        for ab in ability:
            if id == count:
                result = ab.name
            count = count + 1

    if id == -1:
        result = "NO_ABILITY"
    elif id == "NO_ABILITY":
        result = -1
        
    if id == -2:
        result = "RANDOM_ITEM"
    elif id == "RANDOM_ITEM":
        result = -2
        
    if id == -3:
        result = "RANDOM_MAGIC"
    elif id == "RANDOM_MAGIC":
        result = -3

    return result    

def convertRange(range):
    if type(range) == str:
        if range == "LOS":
            result = -1
        else:
            result = range
    else:
        if range == -1:
            result = "LOS"
        else:
            result = range
    return result

def convertDur(dur):
    if type(dur) == str:
        if dur == "PERMANENT":
            result = -1
        else:
            result = dur
    else:
        if dur == -1:
            result = "PERMANENT"
        else:
            result = dur
    return result

def convertColour(colour):
    if type(colour) == str:
        if colour == "TRANSPARENT":
            result = -1    
        elif colour == "BLACK":
            result = 0
        elif colour == "BLUE":
		    result = 1
        elif colour == "LIME":
		    result = 2
        elif colour == "AQUA":
            result = 3
        elif colour == "RED":
            result = 4
        elif colour == "FUCHSIA":
            result = 5
        elif colour == "YELLOW":
            result = 6
        elif colour == "WHITE":
            result = 7
        elif colour == "GRAY":
            result = 8
        elif colour == "NAVY":
            result = 9
        elif colour == "GREEN":
            result = 10
        elif colour == "TEAL":
            result = 11
        elif colour == "MAROON":
            result = 12
        elif colour == "PURPLE":
		    result = 13
        elif colour == "OLIVE":
		    result = 14
        elif colour == "SILVER":
		    result = 15
    else:
        if colour == -1:
            result = "TRANSPARENT"        
        elif colour == 0:
            result = "BLACK"
        elif colour == 1:
		    result = "BLUE"
        elif colour == 2:
		    result = "LIME"
        elif colour == 3:
            result = "AQUA"
        elif colour == 4:
            result = "RED"
        elif colour == 5:
            result = "FUCHSIA"
        elif colour == 6:
            result = "YELLOW"
        elif colour == 7:
            result = "WHITE"
        elif colour == 8:
            result = "GRAY"
        elif colour == 9:
            result = "NAVY"
        elif colour == 10:
            result = "GREEN"
        elif colour == 11:
            result = "TEAL"
        elif colour == 12:
            result = "MAROON"
        elif colour == 13:
		    result = "PURPLE"
        elif colour == 14:
		    result = "OLIVE"
        elif colour == 15:
		    result = "SILVER" 
    return result

def convertNeeds(needs):
    if type(needs) == str:
        if needs == "NONE":
            result = 0
        elif needs == "HEAD":
		    result = 1
        elif needs == "HANDS":
		    result = 2
        elif needs == "HUMANOID":
            result = 3
    else:
        if needs == 0:
            result = "NONE"
        elif needs == 1:
		    result = "HEAD"
        elif needs == 2:
		    result = "HANDS"
        elif needs == 3:
            result = "HUMANOID"
    return result

def convertInvoke(invoke):
    if type(invoke) == str:
        if invoke == "NOT_SELECTABLE":
            result = 0
        elif invoke == "INSTANT_ABILITY":
		    result = 1
        elif invoke == "TARGETABLE_ABILITY":
		    result = 2
        elif invoke == "SELECTABLE":
            result = 3
        elif invoke == "INSTANT_ONESHOT":
            result = 4
    else:
        if invoke == 0:
            result = "NOT_SELECTABLE"
        elif invoke == 1:
		    result = "INSTANT_ABILITY"
        elif invoke == 2:
		    result = "TARGETABLE_ABILITY"
        elif invoke == 3:
            result = "SELECTABLE"
        elif invoke == 4:
            result = "INSTANT_ONESHOT"
    return result


def convertExecute(execute):
    result = 0
    
    if type(execute) == str:
        if execute == "RESIST_HARD":
            result = 1
        elif execute == "RESIST_SHARP":
		    result = 2
        elif execute == "RESIST_ENERGY":
		    result = 3
        elif execute == "RESIST_CHEMICAL":
            result = 4
        elif execute == "CLEAR_ARMOR":
            result = 5
        elif execute == "CLEAR_AMULET":
		    result = 6
        elif execute == "CLEAR_MELEE":
		    result = 7
        elif execute == "MELEE_ATTACK":
            result = 8
        elif execute == "SET":
            result = 9
        elif execute == "MODIFY_SPEED":
		    result = 10
        elif execute == "MODIFY_HEALTH":
		    result = 11
        elif execute == "MODIFY_MAX_HEALTH":
            result = 12
        elif execute == "MODIFY_MAGIC":
		    result = 13
        elif execute == "MODIFY_MAX_MAGIC":
            result = 14
        elif execute == "MODIFY_ATTACK_SKILL":
            result = 15
        elif execute == "MODIFY_DEFENCE_SKILL":
		    result = 16
        elif execute == "INVOKE":
		    result = 18
        elif execute == "EXECUTE":
            result = 19
        elif execute == "CANCEL":
		    result = 20
        elif execute == "RESIST_ABILITY":
		    result = 21
        elif execute == "MODIFY_STEALTH":
            result = 22
        elif execute == "MODIFY_DETECT":
            result = 23
        elif execute == "MODIFY_MISSILE_DEFENCE":
            result = 24
        elif execute == "RESIST_BURST":
            result = 25
    else:
        if execute == 1:
            result = "RESIST_HARD"
        elif execute == 2:
		    result = "RESIST_SHARP"
        elif execute == 3:
		    result = "RESIST_ENERGY"
        elif execute == 4:
            result = "RESIST_CHEMICAL"
        elif execute == 5:
            result = "CLEAR_ARMOR"
        elif execute == 6:
		    result = "CLEAR_AMULET"
        elif execute == 7:
		    result = "CLEAR_MELEE"
        elif execute == 8:
            result = "MELEE_ATTACK"
        elif execute == 9:
            result = "SET"
        elif execute == 10:
		    result = "MODIFY_SPEED"
        elif execute == 11:
		    result = "MODIFY_HEALTH"
        elif execute == 12:
            result = "MODIFY_MAX_HEALTH"            
        elif execute == 13:
            result = "MODIFY_MAGIC"
        elif execute == 14:
            result = "MODIFY_MAX_MAGIC"
        elif execute == 15:
		    result = "MODIFY_ATTACK_SKILL"
        elif execute == 16:
		    result = "MODIFY_DEFENCE_SKILL"
        elif execute == 18:
            result = "INVOKE"
        elif execute == 19:
            result = "EXECUTE"
        elif execute == 20:
		    result = "CANCEL"     
        elif execute == 21:
		    result = "RESIST_ABILITY"  
        elif execute == 22:
            result = "MODIFY_STEALTH" 
        elif execute == 23:
            result = "MODIFY_DETECT" 
        elif execute == 24:
            result = "MODIFY_MISSILE_DEFENCE" 
        elif execute == 25:
            result = "RESIST_BURST"       
    return result

def convertStrategy(strat):
    if type(strat) == str:
        if strat == "NO_STRATEGY":
            result = 0
        elif strat == "VALUE_MODIFIER":
		    result = 1
        elif strat == "VALUE_SETTER":
		    result = 2
        elif strat == "ATTACKER":
            result = 3
        elif strat == "ABILITY_ADDER":
            result = 4
        elif strat == "DESELECT":
            result = 5
        elif strat == "SELECT":
            result = 6
        elif strat == "VALUE_MULTIPLIER":
            result = 7
    else:
        if strat == 0:
            result = "NO_STRATEGY"
        elif strat == 1:
		    result = "VALUE_MODIFIER"
        elif strat == 2:
		    result = "VALUE_SETTER"
        elif strat == 3:
            result = "ATTACKER"
        elif strat == 4:
            result = "ABILITY_ADDER"
        elif strat == 5:
            result = "DESELECT"
        elif strat == 6:
            result = "SELECT"
        elif strat == 7:
            result = "VALUE_MULTIPLIER"
    return result

def convertAttack(attack):
    if type(attack) == str:
        if attack == "NONE":
            result = 0
        elif attack == "HARD":
		    result = 1
        elif attack == "SHARP":
		    result = 2
        elif attack == "ENERGY":
            result = 3
        elif attack == "CHEMICAL":
            result = 4
        elif attack == "KNOCKBACK":
            result = 5
    else:
        if attack == 0:
            result = "NONE"
        elif attack == 1:
		    result = "HARD"
        elif attack == 2:
		    result = "SHARP"
        elif attack == 3:
            result = "ENERGY"
        elif attack == 4:
            result = "CHEMICAL"
        elif attack == 5:
            result = "KNOCKBACK"
    return result

def convertClear(clear):
    if type(clear) == str:
        if clear == "CLEAR_NONE":
            result = 26
        elif clear == "CLEAR_ARMOR":
            result = 5
        elif clear == "CLEAR_AMULET":
		    result = 6
        elif clear == "CLEAR_MELEE":
		    result = 7
    else:
        if clear == 5:
            result = "CLEAR_ARMOR"
        elif clear == 6:
		    result = "CLEAR_AMULET"
        elif clear == 7:
		    result = "CLEAR_MELEE"
        elif clear == 26:
            result = "CLEAR_NONE"
    return result

def parseAbility(line):
    l1 = string.replace(line," ", "_")
    l2 = string.replace(l1, '"', " ")
    l3 = string.replace(l2, "+", " ")
    l4 = string.replace(l3, "," , " ")
    
    parameters = string.split(l4)
    
    if parameters[0] == "****":
        return None
    
    anAbility = Ability()
    
    anAbility.name = parameters[0]
    anAbility.gifname  = parameters[1]
    anAbility.colour = int(parameters[2])
    anAbility.needs = int(parameters[3])
    anAbility.invokingStrategy = int(parameters[4])
    anAbility.duration = int(parameters[5])
    anAbility.physicalItem = int(parameters[6])
    anAbility.magicCost = int(parameters[7])
    anAbility.aimed = int(parameters[8])
    anAbility.value = int(parameters[9])
    anAbility.offensive = int(parameters[10])
    anAbility.cooldown = int(parameters[11])
    anAbility.commands = []
    
    # now we have the basic attributes of the ability, we need to 
    # read an optional list of commands
    
    paramIndex = 12
    
    while string.find(parameters[paramIndex], "*") == -1: # comment not found, so must be command
        aCommand = Command()
        aCommand.name = int(parameters[paramIndex])
        paramIndex = paramIndex + 1
        aCommand.execute = int(parameters[paramIndex])
        paramIndex = paramIndex + 1
        aCommand.param1 = int(parameters[paramIndex])
        paramIndex = paramIndex + 1
        aCommand.param2 = int(parameters[paramIndex])
        paramIndex = paramIndex + 1
        aCommand.param3 = int(parameters[paramIndex])
        paramIndex = paramIndex + 1
        aCommand.param4 = int(parameters[paramIndex])
        paramIndex = paramIndex + 1        
        anAbility.commands.append(aCommand)    
    
    # fill in some empty commands if neccessary
    for i in range(0,6):
        if i not in anAbility.commands:
            aCommand = Command()
            aCommand.name = 0  
            anAbility.commands.append(aCommand)
    
    anAbility.comment = parameters[paramIndex]
    return anAbility
    
    
def parseCreature(line):
    l1 = string.replace(line," ", "_")
    l2 = string.replace(l1, '"', " ")
    l3 = string.replace(l2, "+", " ")
    l4 = string.replace(l3, "," , " ")
    
    parameters = string.split(l4)
    
    aCreature = Creature()
    
    aCreature.name = parameters[0]
    aCreature.gifname  = parameters[1]
    aCreature.colour = int(parameters[2])
    aCreature.head = int(parameters[3])
    aCreature.hands = int(parameters[4])
    aCreature.humanoid = int(parameters[5])
    aCreature.swarm = int(parameters[6])
    aCreature.maxHealth = int(parameters[7])
    aCreature.maxMagic = int(parameters[8])
    aCreature.speed = int(parameters[9])
    aCreature.attack = int(parameters[10])
    aCreature.defence = int(parameters[11])  
    aCreature.value = int(parameters[12])  
    aCreature.stealth = int(parameters[13])  
    aCreature.detect = int(parameters[14]) 
    aCreature.starter = int(parameters[15]) 
    aCreature.abilities = []
    
    # read an optional list of abilities for this creature
    
    paramIndex = 16
    
    while string.find(parameters[paramIndex], "*") == -1: # comment not found, so must be command    
        aCreature.abilities.append(int(parameters[paramIndex]))
        paramIndex = paramIndex + 1

    # fill in some empty abilities if neccessary
    for i in range(0,6):
        if i not in aCreature.abilities:
            aCreature.abilities.append(-1)
        
    aCreature.comment = parameters[paramIndex]
    return aCreature

def nextThing():
    global currentAbility
    global currentCreature
    global creature
    global ability
    clearScreen()    
    if lookingAtAbilities == 1:
        currentAbility = currentAbility + 1
        if currentAbility >= len(ability):
            currentAbility = 0
        ability[currentAbility].draw(currentAbility)
        drawMainButtons()
    else:
        currentCreature = currentCreature + 1
        if currentCreature >= len(creature):
            currentCreature = 0
        creature[currentCreature].draw(currentCreature)
        drawMainButtons()


def prevThing():
    global currentAbility
    global currentCreature
    global creature
    global ability
    clearScreen()    
    if lookingAtAbilities == 1:
        currentAbility = currentAbility - 1
        if currentAbility < 0:
            currentAbility = len(ability) - 1
        ability[currentAbility].draw(currentAbility)
        drawMainButtons()
    else:
        currentCreature = currentCreature - 1
        if currentCreature < 0:
            currentCreature = len(creature) - 1
        creature[currentCreature].draw(currentCreature)
        drawMainButtons()

def searchThing():
    global currentAbility
    global currentCreature
    global creature
    global ability
    global searchString
    global lookingAtAbilities

    searchStr = string.replace(searchString.get()," ", "_")
    print(searchStr)

    for i in range(0, len(ability) - 1):
        if ability[i].name == searchStr:
            clearScreen()
            lookingAtAbilities = 1
            currentAbility = i
            ability[i].draw(currentAbility)
            drawMainButtons()

    for i in range(0, len(creature) - 1):
        if creature[i].name == searchStr:
            clearScreen()
            lookingAtAbilities = 0
            currentCreature = i
            creature[i].draw(currentCreature)
            drawMainButtons()

def clearScreen():
    global win
    global masterWin
    global currentAbility
    global currentCreature
    global creature
    global ability
    if lookingAtAbilities == 1:
        ability[currentAbility].update()
    else:
        creature[currentCreature].update()    
    win.destroy()
    win = Frame(masterWin)
    win.grid()

def exitProg():
    # convert strings back to numerical values, then write back to file here:
    global creature
    global ability
    global masterWin
    clearScreen()
    for ab in ability:
        ab.convert()
        
    for cre in creature:
        cre.convert()
        
    # write to file
    outFile = open("dbash.txt",'w')
    for a in ability:
        a.fileWrite(outFile)
        
    outFile.write("****\n")
    
    for c in creature:
        c.fileWrite(outFile)
    outFile.close()
    
    # now write seperate files in a format suitable for the program
    outFile = open(resString+"a.txt",'w')
    for a in ability:
        a.fileWrite2(outFile)
    outFile.close()
    
    outFile = open(resString+"c.txt",'w')
    for c in creature:
        c.fileWrite2(outFile)
    outFile.close()        
            
    masterWin.destroy()
    print ("finished OK")


def drawMainButtons():
    global win
    global searchString
    back = Button(win, text="        prev        ", command=prevThing)
    back.grid(row=20, column=0)
    next = Button(win, text="        next        ", command=nextThing)
    next.grid(row=20, column=1)
    dele = Button(win, text="delete", command=deleteThing)
    dele.grid(row=20, column=2)
    sl = Label(win, text="search              ")
    sl.grid(row=21, column=0)
    searchString = StringVar()
    se = Entry(win, textvariable=searchString)
    se.grid(row=22, column=0)
    search = Button(win, text="        search        ", command=searchThing)
    search.grid(row=22, column=1)
    
    if lookingAtAbilities == 1:
        tog = Button(win, text="go creatures", command=toggle)
        tog.grid(row=20, column=5)        
        
        cm = Menubutton( win, text="NEW", relief=RAISED)
        cm.grid(row=20, column=6)        
        cm.menu = Menu(cm, tearoff=0)
        cm["menu"] = cm.menu
        for i in ["MELEE WEAPON","MISSILE WEAPON","AMULET","ARMOUR","WAND", "INSTANT ITEM-SELF","INSTANT ITEM-OTHERS","INSTANT ONESHOT-SELF","INSTANT ONESHOT-OTHERS","MAGIC EFFECT","MUNDANE EFFECT", "RESISTANCE"]:
            cm.menu.add_command(label=i, command=lambda i=i: newAbility(i))
    else:
        tog = Button(win, text="go abilities", command=toggle)
        tog.grid(row=20, column=5)           
        new = Button(win, text="NEW", command=newCreature)
        new.grid(row=20, column=6)        

    exit = Button(win, text="exit", command=exitProg)
    exit.grid(row=20, column=7)

def newAbility(ab):
    global currentAbility 
    clearScreen()
    if ab == "MELEE WEAPON":
        line = "new melee,aa,4,0,3,-1,0,0,1,1,1,0,8,3,1,3,-1,0,7,5,0,0,0,0,9,6,7,-1,0,0,*// melee"
    elif ab == "MISSILE WEAPON":
        line = "new missile,ad,6,2,2,-1,1,0,1,1,1,0,19,3,2,10,-1,0,*// missle"
    elif ab == "AMULET":
        line = "new amulet,af,1,1,3,-1,1,0,0,1,0,0,6,5,0,0,0,0,9,6,6,-1,0,0,*// amulet"
    elif ab == "ARMOUR":
        line = "new armour,ab,14,3,3,-1,1,0,0,1,0,0,1,2,30,0,0,0,2,2,30,0,0,0,3,2,30,0,0,0,4,2,30,0,0,0,5,5,0,0,0,0,9,6,5,-1,0,0,*// armour"
    elif ab == "WAND":
        line = "new wand,ac,6,2,2,-1,1,8,1,1,1,0,19,3,3,20,-1,0,*// wand"
    elif ab == "INSTANT ITEM-SELF":
        line = "new instant,ma,5,2,1,-1,1,1,0,1,0,0,19,4,0,0,-1,0,*// instant"
    elif ab == "INSTANT ITEM-OTHERS":
        line = "new instant,ma,5,2,1,-1,1,1,0,1,1,0,19,3,4,5,-1,-1,*// instant"
    elif ab == "INSTANT ONESHOT-SELF":
        line = "new oneshot,ma,5,2,4,-1,1,1,0,1,0,0,19,4,0,0,-1,0,*// oneshot"
    elif ab == "INSTANT ONESHOT-OTHERS":
        line = "new oneshot,ma,5,2,4,-1,1,1,0,1,1,0,19,3,3,40,-1,-1,*// oneshot"
    elif ab == "MAGIC EFFECT":
        line = "new magic fx,st,-1,0,0,20,0,0,0,1,0,0,10,1,2,0,0,0,*// magic fx"
    elif ab == "MUNDANE EFFECT":  
        line = "new mundand fx,ia,-1,0,0,8,0,0,0,1,0,0,11,1,-1,0,0,0,*// mundand fx"
    elif ab == "RESISTANCE":  
        line = "resist ,ia,-1,0,0,0,0,0,0,0,0,0,21,0,-1,0,0,0,*// mundand fx"
                
    na = parseAbility(line)
    na.convert()
    ability.insert(currentAbility+1, na)
    currentAbility = currentAbility + 1
    ability[currentAbility].draw(currentAbility)
    drawMainButtons()   
        
def newCreature():
    global currentCreature 
    clearScreen()
    nc = Creature()
    creature.insert(currentCreature+1, nc)
    currentCreature = currentCreature + 1
    creature[currentCreature].draw(currentCreature)
    drawMainButtons()    

def deleteThing():
    global lookingAtAbilities 
    global currentAbility 
    global currentCreature 
    clearScreen()
    if lookingAtAbilities == 0:
        creature.remove(creature[currentCreature])
        currentCreature = currentCreature - 1
        if currentCreature < 0:
            currentCreature = len(creature) -1
        creature[currentCreature].draw(currentCreature)
        drawMainButtons()
    else:
        ability.remove(ability[currentAbility])
        currentAbility = currentAbility - 1
        if currentAbility < 0:
            currentAbility = len(ability) -1
        ability[currentAbility].draw(currentAbility)
        drawMainButtons()    

def toggle():
    global lookingAtAbilities 
    global currentAbility 
    global currentCreature 
    clearScreen()
    if lookingAtAbilities == 1:
        lookingAtAbilities = 0
        creature[currentCreature].draw(currentCreature)
        drawMainButtons()
    else:
        lookingAtAbilities = 1
        ability[currentAbility].draw(currentAbility)
        drawMainButtons()
        
        
#end of functions

#  START  START  START  START  START  START  START  START  START  START  
#  START  START  START  START  START  START  START  START  START  START  
file = open("dbash.txt",'r')

lines =  file.xreadlines()

for l in lines:
    if parsingAbilities == 1:
        a = parseAbility(l)
        if a == None:
            parsingAbilities = 2
        else:
            ability.append(a)
            abilityIndex = abilityIndex + 1

    if parsingAbilities == 2:
        parsingAbilities = 0
    elif parsingAbilities == 0:
         creature.append(parseCreature(l))
         creatureIndex = creatureIndex + 1

file.close()

# convert all the parameters that need to be converted to meaningful strings
for ab in ability:
    ab.convert()
    
for cre in creature:
    cre.convert()


# at this point we have a list of abilities called ability[] and a list of
# creatures called creature[]
# we need to display these to the user, let the user create new, delete 
# existing, or edit existing abilities
# and/or creatures, then we need to write it back to the file

masterWin = Tk()
win = Frame(masterWin)
win.grid()

ability[currentAbility].draw(currentAbility)
drawMainButtons()
masterWin.mainloop()   