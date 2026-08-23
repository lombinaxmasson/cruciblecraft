"""GT6 RendererBlockTextured empty-state constants and context.

Mirrors gregapi.data.CS pixel/side tables so Java render methods can be
evaluated without Minecraft. Dynamic content (molten, tank fluid, anvil
workpieces, NEI watermarks) is bound to None so those faces drop.
"""
from __future__ import annotations

from typing import Any

PX_OFFSET = 0.005
PX_P = [n / 16.0 for n in range(33)]
PX_N = [1.0 - n / 16.0 for n in range(33)]
PIXELS_POS = PX_P
PIXELS_NEG = PX_N

SIDE_Y_NEG = SIDE_BOTTOM = SIDE_DOWN = 0
SIDE_Y_POS = SIDE_TOP = SIDE_UP = 1
SIDE_Z_NEG = SIDE_NORTH = 2
SIDE_Z_POS = SIDE_SOUTH = SIDE_FRONT = 3
SIDE_X_NEG = SIDE_WEST = 4
SIDE_X_POS = SIDE_EAST = 5
SIDE_ANY = SIDE_UNKNOWN = SIDE_INVALID = SIDE_INSIDE = SIDE_UNDEFINED = 6
SIDE_LEFT = 2
SIDE_RIGHT = 4
SIDE_BACK = 5

FACE_NAMES = ("down", "up", "north", "south", "west", "east")

B = [1 << n for n in range(32)]

ALONG_AXIS = [
    [True, True, False, False, False, False, False, False],
    [True, True, False, False, False, False, False, False],
    [False, False, True, True, False, False, False, False],
    [False, False, True, True, False, False, False, False],
    [False, False, False, False, True, True, False, False],
    [False, False, False, False, True, True, False, False],
    [False, False, False, False, False, False, True, True],
    [False, False, False, False, False, False, True, True],
]
ALONG_AXIS_1 = [
    [False, False, True, True, False, False, False, False],
    [False, False, True, True, False, False, False, False],
    [False, False, False, False, True, True, False, False],
    [False, False, False, False, True, True, False, False],
    [True, True, False, False, False, False, False, False],
    [True, True, False, False, False, False, False, False],
    [False, False, False, False, False, False, False, False],
    [False, False, False, False, False, False, False, False],
]
ALONG_AXIS_2 = [
    [False, False, False, False, True, True, False, False],
    [False, False, False, False, True, True, False, False],
    [True, True, False, False, False, False, False, False],
    [True, True, False, False, False, False, False, False],
    [False, False, True, True, False, False, False, False],
    [False, False, True, True, False, False, False, False],
    [False, False, False, False, False, False, False, False],
    [False, False, False, False, False, False, False, False],
]

SIDES_BOTTOM = [True, False, False, False, False, False, False, False]
SIDES_TOP = [False, True, False, False, False, False, False, False]
SIDES_LEFT = [False, False, True, False, False, False, False, False]
SIDES_FRONT = [False, False, False, True, False, False, False, False]
SIDES_RIGHT = [False, False, False, False, True, False, False, False]
SIDES_BACK = [False, False, False, False, False, True, False, False]
SIDES_INVALID = [False, False, False, False, False, False, True, True]
SIDES_VALID = [True, True, True, True, True, True, False, False]
SIDES_ALL = [True, True, True, True, True, True, True, True]
SIDES_NONE = [False] * 8
SIDES_LEFT_RIGHT = [False, False, True, False, True, False, False, False]
SIDES_FRONT_BACK = [False, False, False, True, False, True, False, False]
SIDES_AXIS_X = [False, False, False, False, True, True, False, False]
SIDES_AXIS_Y = [True, True, False, False, False, False, False, False]
SIDES_AXIS_Z = [False, False, True, True, False, False, False, False]
SIDES_COMPASS = [False, False, True, True, True, True, False, False]
SIDES_VERTICAL = [True, True, False, False, False, False, False, False]
SIDES_HORIZONTAL = [False, False, True, True, True, True, False, False]
SIDES_TOP_HORIZONTAL = [False, True, True, True, True, True, False, False]
SIDES_BOTTOM_HORIZONTAL = [True, False, True, True, True, True, False, False]
SIDES_ITEM_RENDER = [True, True, True, True, True, True, False, False]
ALL_SIDES_VALID = (0, 1, 2, 3, 4, 5)
FACES_TBS = [0, 1, 2, 2, 2, 2, 2, 2]
OPOS = OPPOSITES = [1, 0, 3, 2, 5, 4, 6, 6]


class Keep:
    """Stand-in for a valid GT6 ITexture that should become #texture."""

    def __bool__(self) -> bool:
        return True

    def __eq__(self, other: object) -> bool:
        return isinstance(other, Keep)

    def __ne__(self, other: object) -> bool:
        return not self.__eq__(other)

    def __call__(self, *args: Any, **kwargs: Any) -> Keep:
        return self

    def __getitem__(self, _index: Any) -> Keep:
        return self

    def __getattr__(self, _name: str) -> Keep:
        return self

    def get(self, *args: Any, **kwargs: Any) -> Keep:
        return self

    def __len__(self) -> int:
        return 0

    def __index__(self) -> int:
        return 0

    def __int__(self) -> int:
        return 0

    def __add__(self, other: Any) -> Any:
        return other

    def __radd__(self, other: Any) -> Any:
        return other

    def __sub__(self, other: Any) -> Any:
        return 0 if isinstance(other, (int, float)) else self

    def __rsub__(self, other: Any) -> Any:
        return other

    def __mul__(self, other: Any) -> Any:
        return 0

    def __rmul__(self, other: Any) -> Any:
        return 0

    def __mod__(self, other: Any) -> Any:
        return 0

    def __rmod__(self, other: Any) -> Any:
        return 0

    def __gt__(self, _other: Any) -> bool:
        return False

    def __lt__(self, _other: Any) -> bool:
        return False

    def __ge__(self, _other: Any) -> bool:
        return False

    def __le__(self, _other: Any) -> bool:
        return False

    def __setitem__(self, _index: Any, _value: Any) -> None:
        return None

    def isValidTexture(self) -> bool:
        return True


KEEP = Keep()


class EmptyTank:
    def has(self) -> bool:
        return False

    def isEmpty(self) -> bool:
        return True


class FakeMaterial:
    fRGBaSolid = (0, 0, 0, 0)

    def contains(self, *_args: Any) -> bool:
        return False

    def getTextureSmooth(self, *args: Any, **kwargs: Any) -> Keep:
        return KEEP

    def getTextureSolid(self, *args: Any, **kwargs: Any) -> Keep:
        return KEEP

    def getTextureGem(self, *args: Any, **kwargs: Any) -> Keep:
        return KEEP

    def getTextureMolten(self, *args: Any, **kwargs: Any) -> None:
        return None


class _AttrKeep:
    def __getattr__(self, _name: str) -> Keep:
        return KEEP

    def __getitem__(self, _index: Any) -> Keep:
        return KEEP


class _Code:
    @staticmethod
    def exists(*_args: Any) -> bool:
        return False

    @staticmethod
    def unsignB(value: int) -> int:
        return value & 255

    @staticmethod
    def unsignI(value: int) -> int:
        return value

    @staticmethod
    def bind8(value: int) -> int:
        return max(0, min(255, value))

    @staticmethod
    def getRGBaArray(*_args: Any) -> list[int]:
        return [0, 0, 0, 0]

    @staticmethod
    def getRGBaInt(*_args: Any) -> int:
        return 0

    def __getattr__(self, _name: str) -> Any:
        return lambda *args, **kwargs: 0


class _UT:
    Code = _Code()


class _BlockTextureFluid:
    @staticmethod
    def get(*_args: Any) -> None:
        return None


class _BlockTextureKeep:
    @staticmethod
    def get(*_args: Any, **_kwargs: Any) -> Keep:
        return KEEP


class _BI:
    @staticmethod
    def nei(*_args: Any) -> None:
        return None

    def __getattr__(self, _name: str) -> Any:
        return KEEP


class ZeroIndex(int):
    """Integer that also indexes as an empty display array."""

    def __getitem__(self, _index: Any) -> int:
        return 0


class BoxRecorder:
    def __init__(self) -> None:
        self.box: tuple[float, float, float, float, float, float] | None = None

    def __call__(
        self,
        _block: Any,
        min_x: float,
        min_y: float,
        min_z: float,
        max_x: float,
        max_y: float,
        max_z: float,
    ) -> bool:
        self.box = (
            float(min_x),
            float(min_y),
            float(min_z),
            float(max_x),
            float(max_y),
            float(max_z),
        )
        return True


def constant_namespace() -> dict[str, Any]:
    names = {
        "PX_OFFSET": PX_OFFSET,
        "PX_P": PX_P,
        "PX_N": PX_N,
        "PIXELS_POS": PIXELS_POS,
        "PIXELS_NEG": PIXELS_NEG,
        "SIDE_Y_NEG": SIDE_Y_NEG,
        "SIDE_BOTTOM": SIDE_BOTTOM,
        "SIDE_DOWN": SIDE_DOWN,
        "SIDE_Y_POS": SIDE_Y_POS,
        "SIDE_TOP": SIDE_TOP,
        "SIDE_UP": SIDE_UP,
        "SIDE_Z_NEG": SIDE_Z_NEG,
        "SIDE_NORTH": SIDE_NORTH,
        "SIDE_Z_POS": SIDE_Z_POS,
        "SIDE_SOUTH": SIDE_SOUTH,
        "SIDE_FRONT": SIDE_FRONT,
        "SIDE_X_NEG": SIDE_X_NEG,
        "SIDE_WEST": SIDE_WEST,
        "SIDE_X_POS": SIDE_X_POS,
        "SIDE_EAST": SIDE_EAST,
        "SIDE_ANY": SIDE_ANY,
        "SIDE_UNKNOWN": SIDE_UNKNOWN,
        "SIDE_INVALID": SIDE_INVALID,
        "SIDE_INSIDE": SIDE_INSIDE,
        "SIDE_UNDEFINED": SIDE_UNDEFINED,
        "SIDE_LEFT": SIDE_LEFT,
        "SIDE_RIGHT": SIDE_RIGHT,
        "SIDE_BACK": SIDE_BACK,
        "B": B,
        "ALONG_AXIS": ALONG_AXIS,
        "ALONG_AXIS_1": ALONG_AXIS_1,
        "ALONG_AXIS_2": ALONG_AXIS_2,
        "SIDES_BOTTOM": SIDES_BOTTOM,
        "SIDES_TOP": SIDES_TOP,
        "SIDES_LEFT": SIDES_LEFT,
        "SIDES_FRONT": SIDES_FRONT,
        "SIDES_RIGHT": SIDES_RIGHT,
        "SIDES_BACK": SIDES_BACK,
        "SIDES_INVALID": SIDES_INVALID,
        "SIDES_VALID": SIDES_VALID,
        "SIDES_ALL": SIDES_ALL,
        "SIDES_NONE": SIDES_NONE,
        "SIDES_LEFT_RIGHT": SIDES_LEFT_RIGHT,
        "SIDES_FRONT_BACK": SIDES_FRONT_BACK,
        "SIDES_AXIS_X": SIDES_AXIS_X,
        "SIDES_AXIS_Y": SIDES_AXIS_Y,
        "SIDES_AXIS_Z": SIDES_AXIS_Z,
        "SIDES_COMPASS": SIDES_COMPASS,
        "SIDES_VERTICAL": SIDES_VERTICAL,
        "SIDES_HORIZONTAL": SIDES_HORIZONTAL,
        "SIDES_TOP_HORIZONTAL": SIDES_TOP_HORIZONTAL,
        "SIDES_BOTTOM_HORIZONTAL": SIDES_BOTTOM_HORIZONTAL,
        "SIDES_ITEM_RENDER": SIDES_ITEM_RENDER,
        "ALL_SIDES_VALID": ALL_SIDES_VALID,
        "FACES_TBS": FACES_TBS,
        "OPOS": OPOS,
        "OPPOSITES": OPPOSITES,
        "T": True,
        "F": False,
        "KEEP": KEEP,
        "None": None,
        "True": True,
        "False": False,
    }
    return names


class ExecContext(dict):
    """Mutable render locals; unknown names are seeded before exec."""


def empty_render_context(box: BoxRecorder | None = None) -> ExecContext:
    """Locals for evaluating empty-state render methods."""
    ctx = ExecContext(constant_namespace())
    material = FakeMaterial()
    ctx.update(
        {
            "mFacing": SIDE_Z_POS,
            "mDisplay": ZeroIndex(0),
            "mShape": 0,
            "mShapeA": 0,
            "mShapeB": 0,
            "mMode": 0,
            "mState": 0,
            "mStructureOkay": True,
            "mDisplayedEnergy": 0,
            "mDisplayedHeight": 0,
            "mDisplayedFluid": 0,
            "mMaterialA": 0,
            "mMaterialB": 0,
            "mRGBa": 0,
            "mStyle": 0,
            "mMeltDown": False,
            "mEnergy": 0,
            "mTexture": KEEP,
            "mTextureAnvil": KEEP,
            "mTextureLegs": KEEP,
            "mTextureMolten": None,
            "mTextureA": None,
            "mTextures": [KEEP] * 32,
            "mShelfIcon": KEEP,
            "mTank": EmptyTank(),
            "mTanks": [EmptyTank(), EmptyTank(), EmptyTank(), EmptyTank()],
            "mTanksInput": [],
            "mTanksOutput": [],
            "mMaterial": material,
            "mStone": 0,
            "BI": _BI(),
            "BlockTextureFluid": _BlockTextureFluid(),
            "BlockTextureMulti": _BlockTextureKeep(),
            "BlockTextureDefault": _BlockTextureKeep(),
            "BlockTextureCopied": _BlockTextureKeep(),
            "BlockTextureSided": _BlockTextureKeep(),
            "TD": _AttrKeep(),
            "UT": _UT(),
            "OP": _AttrKeep(),
            "MT": _AttrKeep(),
            "OreDictMaterial": _AttrKeep(),
            "BooksGT": _AttrKeep(),
            "MORTAR_MATERIALS": [material],
            "sEngineColors": [KEEP] * 16,
            "CA_WHITE": KEEP,
            "CA_GRAY_64": KEEP,
            "NI": None,
            "ZL_FLUIDTANKGT": (),
        }
    )
    if box is not None:
        ctx["box"] = box
    return ctx


def texture_is_drawn(value: Any) -> bool:
    if value is None or value is False:
        return False
    return True
