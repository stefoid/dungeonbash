#Boa:Frame:wxFrame1

from wxPython.wx import *
from wxPython.gizmos import *

def create(parent):
    return wxFrame1(parent)

[wxID_WXFRAME1, wxID_WXFRAME1BUTTON1, wxID_WXFRAME1BUTTON10, 
 wxID_WXFRAME1BUTTON11, wxID_WXFRAME1BUTTON12, wxID_WXFRAME1BUTTON13, 
 wxID_WXFRAME1BUTTON14, wxID_WXFRAME1BUTTON2, wxID_WXFRAME1BUTTON3, 
 wxID_WXFRAME1BUTTON4, wxID_WXFRAME1BUTTON5, wxID_WXFRAME1BUTTON6, 
 wxID_WXFRAME1BUTTON7, wxID_WXFRAME1BUTTON8, wxID_WXFRAME1BUTTON9, 
 wxID_WXFRAME1COMBOBOX1, wxID_WXFRAME1COMBOBOX2, wxID_WXFRAME1COMBOBOX3, 
 wxID_WXFRAME1COMBOBOX4, wxID_WXFRAME1COMBOBOX5, wxID_WXFRAME1COMBOBOX6, 
 wxID_WXFRAME1LEDNUMBERCTRL1, wxID_WXFRAME1NEEDS, wxID_WXFRAME1STATICTEXT1, 
 wxID_WXFRAME1STATICTEXT10, wxID_WXFRAME1STATICTEXT11, 
 wxID_WXFRAME1STATICTEXT12, wxID_WXFRAME1STATICTEXT13, 
 wxID_WXFRAME1STATICTEXT14, wxID_WXFRAME1STATICTEXT15, 
 wxID_WXFRAME1STATICTEXT2, wxID_WXFRAME1STATICTEXT3, wxID_WXFRAME1STATICTEXT4, 
 wxID_WXFRAME1STATICTEXT5, wxID_WXFRAME1STATICTEXT6, wxID_WXFRAME1STATICTEXT7, 
 wxID_WXFRAME1STATICTEXT8, wxID_WXFRAME1STATICTEXT9, wxID_WXFRAME1TEXTCTRL1, 
 wxID_WXFRAME1TEXTCTRL2, wxID_WXFRAME1TEXTCTRL3, 
] = map(lambda _init_ctrls: wxNewId(), range(41))

class wxFrame1(wxFrame):
    def _init_ctrls(self, prnt):
        # generated method, don't edit
        wxFrame.__init__(self, id=wxID_WXFRAME1, name='', parent=prnt,
              pos=wxPoint(241, 146), size=wxSize(1030, 843),
              style=wxDEFAULT_FRAME_STYLE, title='wxFrame1')
        self.SetClientSize(wxSize(1022, 815))

        self.comboBox1 = wxComboBox(choices=[], id=wxID_WXFRAME1COMBOBOX1,
              name='comboBox1', parent=self, pos=wxPoint(320, 88),
              size=wxSize(104, 24), style=0, value='comboBox1')

        self.needs = wxComboBox(choices=[], id=wxID_WXFRAME1NEEDS, name='needs',
              parent=self, pos=wxPoint(448, 88), size=wxSize(104, 24), style=0,
              value='comboBox2')

        self.button1 = wxButton(id=wxID_WXFRAME1BUTTON1, label='PREV',
              name='button1', parent=self, pos=wxPoint(232, 16), size=wxSize(87,
              28), style=0)

        self.button2 = wxButton(id=wxID_WXFRAME1BUTTON2, label='NEXT',
              name='button2', parent=self, pos=wxPoint(336, 16), size=wxSize(87,
              28), style=0)

        self.button3 = wxButton(id=wxID_WXFRAME1BUTTON3, label='new melee',
              name='button3', parent=self, pos=wxPoint(48, 640), size=wxSize(87,
              28), style=0)

        self.button4 = wxButton(id=wxID_WXFRAME1BUTTON4, label='new missile',
              name='button4', parent=self, pos=wxPoint(144, 640),
              size=wxSize(87, 28), style=0)

        self.button5 = wxButton(id=wxID_WXFRAME1BUTTON5, label='new armour',
              name='button5', parent=self, pos=wxPoint(240, 640),
              size=wxSize(87, 28), style=0)

        self.button6 = wxButton(id=wxID_WXFRAME1BUTTON6, label='new amulet',
              name='button6', parent=self, pos=wxPoint(336, 640),
              size=wxSize(87, 28), style=0)

        self.button7 = wxButton(id=wxID_WXFRAME1BUTTON7, label='new wand',
              name='button7', parent=self, pos=wxPoint(432, 640),
              size=wxSize(87, 28), style=0)

        self.button8 = wxButton(id=wxID_WXFRAME1BUTTON8, label='new instant',
              name='button8', parent=self, pos=wxPoint(528, 640),
              size=wxSize(87, 28), style=0)

        self.button9 = wxButton(id=wxID_WXFRAME1BUTTON9, label='new oneshot',
              name='button9', parent=self, pos=wxPoint(624, 640),
              size=wxSize(87, 28), style=0)

        self.button10 = wxButton(id=wxID_WXFRAME1BUTTON10,
              label='new magic eff', name='button10', parent=self,
              pos=wxPoint(720, 640), size=wxSize(87, 28), style=0)

        self.button11 = wxButton(id=wxID_WXFRAME1BUTTON11, label='new nonmagic',
              name='button11', parent=self, pos=wxPoint(816, 640),
              size=wxSize(87, 28), style=0)

        self.button12 = wxButton(id=wxID_WXFRAME1BUTTON12, label='DELETE',
              name='button12', parent=self, pos=wxPoint(576, 16),
              size=wxSize(87, 28), style=0)

        self.button13 = wxButton(id=wxID_WXFRAME1BUTTON13, label='SAVE-EXIT',
              name='button13', parent=self, pos=wxPoint(800, 16),
              size=wxSize(87, 28), style=0)

        self.staticText1 = wxStaticText(id=wxID_WXFRAME1STATICTEXT1,
              label='colour', name='staticText1', parent=self, pos=wxPoint(352,
              64), size=wxSize(37, 16), style=0)

        self.button14 = wxButton(id=wxID_WXFRAME1BUTTON14,
              label='Ability/Creature', name='button14', parent=self,
              pos=wxPoint(16, 16), size=wxSize(120, 28), style=0)

        self.textCtrl1 = wxTextCtrl(id=wxID_WXFRAME1TEXTCTRL1, name='textCtrl1',
              parent=self, pos=wxPoint(72, 88), size=wxSize(136, 24), style=0,
              value='textCtrl1')

        self.staticText2 = wxStaticText(id=wxID_WXFRAME1STATICTEXT2,
              label='name', name='staticText2', parent=self, pos=wxPoint(112,
              64), size=wxSize(34, 16), style=0)

        self.textCtrl2 = wxTextCtrl(id=wxID_WXFRAME1TEXTCTRL2, name='textCtrl2',
              parent=self, pos=wxPoint(224, 88), size=wxSize(76, 24), style=0,
              value='textCtrl2')

        self.staticText3 = wxStaticText(id=wxID_WXFRAME1STATICTEXT3,
              label='gifname', name='staticText3', parent=self, pos=wxPoint(232,
              64), size=wxSize(48, 16), style=0)

        self.staticText4 = wxStaticText(id=wxID_WXFRAME1STATICTEXT4,
              label='ID num', name='staticText4', parent=self, pos=wxPoint(16,
              64), size=wxSize(41, 16), style=0)

        self.staticText5 = wxStaticText(id=wxID_WXFRAME1STATICTEXT5,
              label='needs', name='staticText5', parent=self, pos=wxPoint(480,
              64), size=wxSize(38, 16), style=0)

        self.staticText6 = wxStaticText(id=wxID_WXFRAME1STATICTEXT6,
              label='invoke strat', name='staticText6', parent=self,
              pos=wxPoint(584, 64), size=wxSize(68, 16), style=0)

        self.comboBox2 = wxComboBox(choices=[], id=wxID_WXFRAME1COMBOBOX2,
              name='comboBox2', parent=self, pos=wxPoint(576, 88),
              size=wxSize(130, 24), style=0, value='comboBox2')

        self.staticText7 = wxStaticText(id=wxID_WXFRAME1STATICTEXT7,
              label='duration', name='staticText7', parent=self,
              pos=wxPoint(736, 64), size=wxSize(48, 16), style=0)

        self.staticText8 = wxStaticText(id=wxID_WXFRAME1STATICTEXT8,
              label='magic cost', name='staticText8', parent=self,
              pos=wxPoint(824, 64), size=wxSize(65, 16), style=0)

        self.comboBox3 = wxComboBox(choices=[], id=wxID_WXFRAME1COMBOBOX3,
              name='comboBox3', parent=self, pos=wxPoint(720, 88),
              size=wxSize(96, 24), style=0, value='comboBox3')

        self.textCtrl3 = wxTextCtrl(id=wxID_WXFRAME1TEXTCTRL3, name='textCtrl3',
              parent=self, pos=wxPoint(832, 88), size=wxSize(64, 24), style=0,
              value='textCtrl3')

        self.lEDNumberCtrl1 = wxLEDNumberCtrl(id=wxID_WXFRAME1LEDNUMBERCTRL1,
              parent=self, pos=wxPoint(24, 88), size=wxSize(32, 24),
              style=wxLED_ALIGN_LEFT)

        self.staticText9 = wxStaticText(id=wxID_WXFRAME1STATICTEXT9,
              label='aimed?', name='staticText9', parent=self, pos=wxPoint(928,
              64), size=wxSize(45, 16), style=0)

        self.comboBox4 = wxComboBox(choices=[], id=wxID_WXFRAME1COMBOBOX4,
              name='comboBox4', parent=self, pos=wxPoint(912, 88),
              size=wxSize(80, 24), style=0, value='comboBox4')

        self.staticText10 = wxStaticText(id=wxID_WXFRAME1STATICTEXT10,
              label='command', name='staticText10', parent=self,
              pos=wxPoint(104, 192), size=wxSize(60, 16), style=0)

        self.staticText11 = wxStaticText(id=wxID_WXFRAME1STATICTEXT11,
              label='execute', name='staticText11', parent=self,
              pos=wxPoint(240, 192), size=wxSize(47, 16), style=0)

        self.staticText12 = wxStaticText(id=wxID_WXFRAME1STATICTEXT12,
              label='param1', name='staticText12', parent=self, pos=wxPoint(440,
              192), size=wxSize(46, 16), style=0)

        self.staticText13 = wxStaticText(id=wxID_WXFRAME1STATICTEXT13,
              label='param2', name='staticText13', parent=self, pos=wxPoint(592,
              192), size=wxSize(46, 16), style=0)

        self.staticText14 = wxStaticText(id=wxID_WXFRAME1STATICTEXT14,
              label='param3', name='staticText14', parent=self, pos=wxPoint(712,
              192), size=wxSize(46, 16), style=0)

        self.staticText15 = wxStaticText(id=wxID_WXFRAME1STATICTEXT15,
              label='param4', name='staticText15', parent=self, pos=wxPoint(832,
              192), size=wxSize(46, 16), style=0)

        self.comboBox5 = wxComboBox(choices=[], id=wxID_WXFRAME1COMBOBOX5,
              name='comboBox5', parent=self, pos=wxPoint(208, 224),
              size=wxSize(144, 24), style=0, value='comboBox5')

        self.comboBox6 = wxComboBox(choices=[], id=wxID_WXFRAME1COMBOBOX6,
              name='comboBox6', parent=self, pos=wxPoint(40, 224),
              size=wxSize(154, 24), style=0, value='comboBox6')

    def __init__(self, parent):
        self._init_ctrls(parent)
