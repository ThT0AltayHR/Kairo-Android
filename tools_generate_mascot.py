from PIL import Image, ImageDraw
from pathlib import Path
import math
OUT=Path(__file__).parent/'app/src/main/assets'
OUT.mkdir(parents=True,exist_ok=True)
S=384; N=10
MOODS={
 'idle':(0xA7,0xC7,0xFF), 'thinking':(0xC2,0xA2,0xFF), 'done':(0x83,0xE6,0xC2), 'error':(0xFF,0x99,0x87), 'question':(0xFF,0xD1,0x85)
}
def frame(mood,i):
    t=i/N*2*math.pi
    im=Image.new('RGBA',(S,S),(0,0,0,0)); d=ImageDraw.Draw(im)
    r,g,b=MOODS[mood]
    bob=int(8*math.sin(t)) if mood in ('idle','thinking') else int(-14*abs(math.sin(t))) if mood=='done' else 0
    # Ambient halo, subtle and cropped safely.
    d.ellipse((42,38+bob,342,338+bob),fill=(r,g,b,22))
    # Small asymmetric antenna and its star tip.
    ax,ay=278,76+bob
    d.line((ax,ay+33,ax+11,ay-12),fill=(205,218,255,245),width=15)
    d.ellipse((ax-1,ay-21,ax+22,ay+2),fill=(r,g,b,255))
    # Plush wings / side fins.
    flap=int(10*math.sin(t+0.3))
    d.ellipse((50,174+bob+flap,136,252+bob+flap),fill=(146,177,236,255),outline=(229,236,255,255),width=6)
    d.ellipse((248,174+bob-flap,334,252+bob-flap),fill=(146,177,236,255),outline=(229,236,255,255),width=6)
    # Cloud-fox body: not a square, with soft lobes and hand-drawn outline.
    d.ellipse((86,79+bob,298,310+bob),fill=(245,244,255,255),outline=(226,226,248,255),width=8)
    d.ellipse((75,123+bob,167,254+bob),fill=(245,244,255,255))
    d.ellipse((224,119+bob,315,251+bob),fill=(245,244,255,255))
    # Warm belly patch and a small star badge (no headband).
    d.ellipse((132,204+bob,253,295+bob),fill=(220,225,246,255))
    d.ellipse((244,246+bob,282,284+bob),fill=(r,g,b,255))
    # Face mood.
    blink=(mood=='idle' and i in (4,5))
    if mood=='error':
        # tired downturned eyes
        d.arc((133,160+bob,166,184+bob),190,345,fill=(43,48,70,255),width=8)
        d.arc((214,160+bob,247,184+bob),190,345,fill=(43,48,70,255),width=8)
    elif blink:
        d.line((137,172+bob,161,172+bob),fill=(43,48,70,255),width=8)
        d.line((219,172+bob,243,172+bob),fill=(43,48,70,255),width=8)
    else:
        pupil_shift=5*math.sin(t) if mood=='thinking' else 0
        d.ellipse((136+pupil_shift,156+bob,163+pupil_shift,186+bob),fill=(39,43,64,255))
        d.ellipse((219+pupil_shift,156+bob,246+pupil_shift,186+bob),fill=(39,43,64,255))
        d.ellipse((142+pupil_shift,161+bob,150+pupil_shift,169+bob),fill=(255,255,255,255))
        d.ellipse((225+pupil_shift,161+bob,233+pupil_shift,169+bob),fill=(255,255,255,255))
    d.ellipse((112,191+bob,138,206+bob),fill=(255,160,178,110))
    d.ellipse((245,191+bob,271,206+bob),fill=(255,160,178,110))
    if mood=='done':
        d.arc((175,179+bob,211,207+bob),5,170,fill=(74,124,114,255),width=7)
    elif mood=='error':
        d.arc((176,192+bob,210,222+bob),190,350,fill=(107,81,88,255),width=6)
    elif mood=='question':
        d.ellipse((180,187+bob,204,211+bob),outline=(126,92,47,255),width=5); d.ellipse((189,215+bob,195,221+bob),fill=(126,92,47,255))
    else:
        d.arc((177,179+bob,208,205+bob),8,168,fill=(105,112,141,255),width=5)
    # State-specific little glints.
    if mood=='thinking':
        for j in range(3):
            a=t+j*2.1; x=192+int(139*math.cos(a)); y=180+int(114*math.sin(a))
            rr=6+(j%2)*3; d.ellipse((x-rr,y-rr,x+rr,y+rr),fill=(r,g,b,220))
    elif mood=='done':
        x=192+int(20*math.sin(t)); y=56+int(5*math.cos(t))
        d.polygon([(x,y-16),(x+5,y-5),(x+16,y),(x+5,y+5),(x,y+16),(x-5,y+5),(x-16,y),(x-5,y-5)],fill=(r,g,b,245))
    elif mood=='question':
        d.ellipse((95,91,107,103),fill=(r,g,b,245)); d.ellipse((294,108,306,120),fill=(r,g,b,245))
    # Gentle internal fabric shine.
    d.arc((102,94+bob,276,281+bob),198,268,fill=(255,255,255,150),width=5)
    return im.resize((160,160),Image.Resampling.LANCZOS)
for mood in MOODS:
    frames=[frame(mood,i) for i in range(N)]
    frames[0].save(OUT/f'mascot_{mood}.gif',save_all=True,append_images=frames[1:],duration=125,loop=0,disposal=2,optimize=True,transparency=0)
print('generated',len(MOODS),'original animated mascot GIFs')
