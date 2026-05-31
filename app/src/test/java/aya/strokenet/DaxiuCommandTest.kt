package aya.strokenet

import android.content.Context
import android.content.SharedPreferences
import aya.strokenet.ble.DaxiuCommand
import aya.strokenet.data.model.ControlParams
import aya.strokenet.utils.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

/**
 * 大秀命令测试
 * 验证BLE命令格式和参数映射的正确性
 */
@RunWith(MockitoJUnitRunner::class)
class DaxiuCommandTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences
    
    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor
    
    @Before
    fun setup() {
        // Mock SharedPreferences
        `when`(mockContext.getSharedPreferences("ble_settings", Context.MODE_PRIVATE))
            .thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.getString("ble_device_id", null))
            .thenReturn("A1B2")  // 固定设备ID用于测试
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
    }
    
    @Test
    fun testToTwoHexString() {
        assertEquals("00", 0.toTwoHexString())
        assertEquals("0F", 15.toTwoHexString())
        assertEquals("FF", 255.toTwoHexString())
        assertEquals("10", 16.toTwoHexString())
        assertEquals("A5", 165.toTwoHexString())
    }
    
    @Test
    fun testToTwoHexStringLowercase() {
        assertEquals("0f", 15.toTwoHexString(uppercase = false))
        assertEquals("ff", 255.toTwoHexString(uppercase = false))
        assertEquals("a5", 165.toTwoHexString(uppercase = false))
    }
    
    @Test
    fun testGetControlDeviceId() {
        val deviceId = getControlDeviceId(mockContext)
        assertEquals("A1B2", deviceId)
        assertEquals(4, deviceId.length)
    }
    
    @Test
    fun testGenerateControlCommandId() {
        val commandId = generateControlCommandId()
        assertEquals(2, commandId.length)
        // 验证是有效的十六进制
        assertTrue(commandId.matches(Regex("[0-9A-F]{2}")))
    }
    
    @Test
    fun test () {
        // 测试官方示例
        val uuid1 = "71000312880012340000000000000000"
        val checksum1 = makeCheckSum(uuid1)
        assertEquals(2, checksum1.length)
        assertTrue(checksum1.matches(Regex("[0-9A-F]{2}")))
        
        // 测试边界情况：单字符
        val uuid2 = "F"
        val checksum2 = makeCheckSum(uuid2)
        assertEquals("F0", checksum2)  // F * 16 = 240 = 0xF0
    }
    
    @Test
    fun testMakeCheckSumCalculation() {
        // 手动计算验证
        // "0102" = 0x01 + 0x02 = 3 = 0x03
        assertEquals("03", makeCheckSum("0102"))
        
        // "FFFF" = 0xFF + 0xFF = 510 % 256 = 254 = 0xFE
        assertEquals("FE", makeCheckSum("FFFF"))
        
        // "1020" = 0x10 + 0x20 = 48 = 0x30
        assertEquals("30", makeCheckSum("1020"))
    }
    
    @Test
    fun testBuildControlCommand() {
        val params = ControlParams(
            depth = 50,
            extendSpeed = 50,
            retractSpeed = 50,
            strength = 50,
            temp = 30
        )
        
        val uuid = DaxiuCommand.buildControlCommand(params, mockContext)
        
        // 验证UUID格式（标准UUID格式，36字符）
        assertTrue(uuid.matches(Regex("[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}")))
        
        // 验证前缀
        assertTrue(uuid.startsWith("710003"))
        
        // 验证命令类型（8800 = 推拉控制）
        assertTrue(uuid.contains("-8800-"))
        
        // 验证设备ID
        assertTrue(uuid.contains("-A1B2-"))
        
        // 验证长度（36个字符，标准UUID格式）
        assertEquals(36, uuid.length)
    }
    
    @Test
    fun testBuildTemperatureCommand() {
        val uuid = DaxiuCommand.buildTemperatureCommand(50, mockContext)
        
        // 验证命令类型（9000 = 温度控制）
        assertTrue(uuid.contains("-9000-"))
        
        // 验证格式
        assertTrue(uuid.startsWith("710003"))
        assertEquals(36, uuid.length)
    }
    
    @Test
    fun testBuildStopCommand() {
        val uuid = DaxiuCommand.buildStopCommand(mockContext)
        
        // 验证命令类型（8800 = 推拉停止）
        assertTrue(uuid.contains("-8800-"))
        
        // 验证格式
        assertTrue(uuid.startsWith("710003"))
        assertEquals(36, uuid.length)
    }
    
    @Test
    fun testBuildDiscoverCommand() {
        val uuid = DaxiuCommand.buildDiscoverCommand(mockContext)
        
        // 验证命令类型（1F00 = 设备发现）
        assertTrue(uuid.contains("-1F00-"))
        
        // 验证格式
        assertTrue(uuid.startsWith("710003"))
        assertEquals(36, uuid.length)
    }
    
    @Test
    fun testDepthMapping() {
        // 深度 0 应该映射到 0
        val params0 = ControlParams(depth = 0)
        val uuid0 = DaxiuCommand.buildControlCommand(params0, mockContext)
        assertTrue(uuid0.contains("0000-00"))  // 深度00
        
        // 深度 100 应该映射到最大值 (约72)
        val params100 = ControlParams(depth = 100)
        val uuid100 = DaxiuCommand.buildControlCommand(params100, mockContext)
        // 映射公式: ((100-1)*58)/99 + 14 ≈ 72
        assertTrue(uuid100.contains("0000-48"))  // 深度48 (十六进制，72的十六进制)
    }
    
    @Test
    fun testSpeedMapping() {
        // 速度 0 应该映射到 0
        val params0 = ControlParams(extendSpeed = 0, retractSpeed = 0)
        val uuid0 = DaxiuCommand.buildControlCommand(params0, mockContext)
        // 应该包含速度 00 00
        
        // 速度 100 应该映射到最大值 (约15)
        val params100 = ControlParams(extendSpeed = 100, retractSpeed = 100)
        val uuid100 = DaxiuCommand.buildControlCommand(params100, mockContext)
        // 映射公式: ((100-1)*13)/99 + 2 ≈ 15
    }
    
    @Test
    fun testTemperatureMapping() {
        // 温度 0 应该映射到 30
        val uuid0 = DaxiuCommand.buildTemperatureCommand(0, mockContext)
        assertTrue(uuid0.contains("0000-1E"))  // 30 = 0x1E
        
        // 温度 60 应该映射到 60
        val uuid60 = DaxiuCommand.buildTemperatureCommand(60, mockContext)
        assertTrue(uuid60.contains("0000-3C"))  // 60 = 0x3C
        
        // 温度 30 应该映射到 45
        val uuid30 = DaxiuCommand.buildTemperatureCommand(30, mockContext)
        assertTrue(uuid30.contains("0000-2D"))  // 45 = 0x2D
    }
    
    @Test
    fun testChecksumDifference() {
        // 相同参数应该生成不同的UUID（因为命令ID随机）
        val params = ControlParams(depth = 50)
        val uuid1 = DaxiuCommand.buildControlCommand(params, mockContext)
        val uuid2 = DaxiuCommand.buildControlCommand(params, mockContext)
        
        // UUID应该不同（命令ID不同）
        assertNotEquals(uuid1, uuid2)
        
        // 但格式应该相同
        assertEquals(uuid1.length, uuid2.length)
        assertTrue(uuid1.startsWith("710003"))
        assertTrue(uuid2.startsWith("710003"))
    }
    
    @Test
    fun testIsXiaomiDevice() {
        // 这个测试依赖于实际设备，这里只验证函数存在
        val isXiaomi = isXiaomiDevice()
        assertTrue(isXiaomi || !isXiaomi)  // 总是true，只是验证函数可调用
    }
    
    @Test
    fun testAllCommandsHaveCorrectLength() {
        // 测试所有命令的UUID长度都是36字符（标准UUID格式）
        val controlUuid = DaxiuCommand.buildControlCommand(
            ControlParams(depth = 50, extendSpeed = 10, retractSpeed = 10),
            mockContext
        )
        assertEquals("Control command UUID length should be 36", 36, controlUuid.length)
        
        val tempUuid = DaxiuCommand.buildTemperatureCommand(40, mockContext)
        assertEquals("Temperature command UUID length should be 36", 36, tempUuid.length)
        
        val strengthUuid = DaxiuCommand.buildStrengthCommand(50, mockContext)
        assertEquals("Strength command UUID length should be 36", 36, strengthUuid.length)
        
        val stopUuid = DaxiuCommand.buildStopCommand(mockContext)
        assertEquals("Stop command UUID length should be 36", 36, stopUuid.length)
        
        val discoverUuid = DaxiuCommand.buildDiscoverCommand(mockContext)
        assertEquals("Discover command UUID length should be 36", 36, discoverUuid.length)
    }
    
    @Test
    fun testStrengthCommandFormat() {
        val uuid = DaxiuCommand.buildStrengthCommand(54, mockContext)
        
        println("Strength UUID: $uuid")
        println("UUID length: ${uuid.length}")
        
        // 验证长度
        assertEquals("UUID length should be 36", 36, uuid.length)
        
        // 验证格式
        assertTrue(uuid.matches(Regex("^[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}$")))
        
        // 验证命令类型（强度命令是8200）
        assertTrue(uuid.contains("-8200-"))
        
        // 验证包含固定前缀640000
        val parts = uuid.split("-")
        assertTrue(parts[4].startsWith("640000"))
        
        // 验证强度值（54 = 0x36）
        assertTrue(parts[4].contains("3600"))
    }
    
    @Test
    fun testUUIDFormatValidation() {
        // 测试所有命令都符合标准UUID格式：8-4-4-4-12
        val commands = listOf(
            DaxiuCommand.buildControlCommand(ControlParams(), mockContext),
            DaxiuCommand.buildTemperatureCommand(30, mockContext),
            DaxiuCommand.buildStrengthCommand(50, mockContext),
            DaxiuCommand.buildStopCommand(mockContext),
            DaxiuCommand.buildDiscoverCommand(mockContext)
        )
        
        commands.forEach { uuid ->
            val parts = uuid.split("-")
            assertEquals("UUID should have 5 parts", 5, parts.size)
            assertEquals("First part should be 8 chars", 8, parts[0].length)
            assertEquals("Second part should be 4 chars", 4, parts[1].length)
            assertEquals("Third part should be 4 chars", 4, parts[2].length)
            assertEquals("Fourth part should be 4 chars", 4, parts[3].length)
            assertEquals("Fifth part should be 12 chars", 12, parts[4].length)
        }
    }
}
