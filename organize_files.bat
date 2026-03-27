@echo off
REM Run this script from the folder containing all the .java files
REM It will create sub-folders and move files to the right place

set BASE=src\main\java\com\trolmastercard\sexmod

mkdir %BASE%\client
mkdir %BASE%\client\anim
mkdir %BASE%\client\event
mkdir %BASE%\client\gui
mkdir %BASE%\client\layer
mkdir %BASE%\client\model
mkdir %BASE%\client\particle
mkdir %BASE%\client\render
mkdir %BASE%\client\render\layer
mkdir %BASE%\client\renderer
mkdir %BASE%\client\screen
mkdir %BASE%\client\util
mkdir %BASE%\command
mkdir %BASE%\data
mkdir %BASE%\entity
mkdir %BASE%\entity\ai
mkdir %BASE%\event
mkdir %BASE%\handler
mkdir %BASE%\inventory
mkdir %BASE%\item
mkdir %BASE%\network
mkdir %BASE%\network\packet
mkdir %BASE%\potion
mkdir %BASE%\registry
mkdir %BASE%\tribe
mkdir %BASE%\util
mkdir %BASE%\world

REM client
move %BASE%\FakeWorld.java %BASE%\client\
move %BASE%\OutlineShaderManager.java %BASE%\client\
move %BASE%\SexProposalManager.java %BASE%\client\
move %BASE%\VersionChecker.java %BASE%\client\
move %BASE%\ClientStateManager.java %BASE%\client\
move %BASE%\HornyMeterOverlay.java %BASE%\client\
move %BASE%\PlayerCamEventHandler.java %BASE%\client\
move %BASE%\PlayerKoboldRenderHandler.java %BASE%\client\
move %BASE%\StartupInitFrame.java %BASE%\client\

REM client.anim
move %BASE%\CachedAnimationProcessor.java %BASE%\client\anim\
move %BASE%\CustomAnimationController.java %BASE%\client\anim\

REM client.event
move %BASE%\MenuClearHandler.java %BASE%\client\event\
move %BASE%\NpcRenderEventHandler.java %BASE%\client\event\

REM client.gui
move %BASE%\ClothingScrollWidget.java %BASE%\client\gui\
move %BASE%\NpcCustomizeScreen.java %BASE%\client\gui\
move %BASE%\NpcEquipmentScreen.java %BASE%\client\gui\
move %BASE%\NpcInventoryScreen.java %BASE%\client\gui\

REM client.layer
move %BASE%\ElytraLayer.java %BASE%\client\layer\

REM client.model
move %BASE%\AllieLampModel.java %BASE%\client\model\
move %BASE%\AllieModel.java %BASE%\client\model\
move %BASE%\BaseNpcModel.java %BASE%\client\model\
move %BASE%\BeeModel.java %BASE%\client\model\
move %BASE%\BiaModel.java %BASE%\client\model\
move %BASE%\CachedGeoModel.java %BASE%\client\model\
move %BASE%\CatModel.java %BASE%\client\model\
move %BASE%\ClothingOverlayModel.java %BASE%\client\model\
move %BASE%\CustomModelManager.java %BASE%\client\model\
move %BASE%\EggModel.java %BASE%\client\model\
move %BASE%\EllieModel.java %BASE%\client\model\
move %BASE%\EnderPearlModel.java %BASE%\client\model\
move %BASE%\GalathCoinModel.java %BASE%\client\model\
move %BASE%\GalathModel.java %BASE%\client\model\
move %BASE%\GoblinModel.java %BASE%\client\model\
move %BASE%\IBoneAccessor.java %BASE%\client\model\
move %BASE%\IBoneFilter.java %BASE%\client\model\
move %BASE%\JennyModel.java %BASE%\client\model\
move %BASE%\KoboldEggGeoModel.java %BASE%\client\model\
move %BASE%\KoboldEggItemModel.java %BASE%\client\model\
move %BASE%\KoboldModel.java %BASE%\client\model\
move %BASE%\KoboldStaffModel.java %BASE%\client\model\
move %BASE%\MangleLieModel.java %BASE%\client\model\
move %BASE%\PropModel.java %BASE%\client\model\
move %BASE%\SlimeModel.java %BASE%\client\model\
move %BASE%\SpearGripBoneModel.java %BASE%\client\model\
move %BASE%\SpearModel.java %BASE%\client\model\
move %BASE%\SpearModelAlt.java %BASE%\client\model\
move %BASE%\SpearTipBoneModel.java %BASE%\client\model\
move %BASE%\StaffHeadBoneModel.java %BASE%\client\model\
move %BASE%\StaffModel.java %BASE%\client\model\
move %BASE%\WinchesterModel.java %BASE%\client\model\
move %BASE%\WispFaceModel.java %BASE%\client\model\
move %BASE%\FishingRodBoneModel.java %BASE%\client\model\

REM client.particle
move %BASE%\SexmodDragonBreathParticle.java %BASE%\client\particle\

REM client.render
move %BASE%\EnergyBallRenderer.java %BASE%\client\render\
move %BASE%\NpcBoneQuadBuilder.java %BASE%\client\render\
move %BASE%\TubeRenderer.java %BASE%\client\render\

REM client.render.layer
move %BASE%\KoboldEggOuterLayer.java %BASE%\client\render\layer\

REM client.renderer
move %BASE%\AlliesLampItemRenderer.java %BASE%\client\renderer\
move %BASE%\BeeBodyRenderer.java %BASE%\client\renderer\
move %BASE%\BlockHighlightRenderer.java %BASE%\client\renderer\
move %BASE%\KoboldEggRenderer.java %BASE%\client\renderer\
move %BASE%\KoboldRenderer.java %BASE%\client\renderer\
move %BASE%\MangleLieSexRenderer.java %BASE%\client\renderer\
move %BASE%\NpcHandRenderer.java %BASE%\client\renderer\
move %BASE%\StaffItemRenderer.java %BASE%\client\renderer\
move %BASE%\GalathCoinRenderer.java %BASE%\client\renderer\
move %BASE%\GalathRenderer.java %BASE%\client\renderer\
move %BASE%\KoboldEggEntityRenderer.java %BASE%\client\renderer\
move %BASE%\KoboldEggItemRenderer.java %BASE%\client\renderer\
move %BASE%\MangleLieRenderer.java %BASE%\client\renderer\

REM client.screen
move %BASE%\BeeQuickAccessScreen.java %BASE%\client\screen\
move %BASE%\NpcTypeSelectScreen.java %BASE%\client\screen\

REM client.util
move %BASE%\NpcRenderUtil.java %BASE%\client\util\

REM command
move %BASE%\FutaCommand.java %BASE%\command\
move %BASE%\LocateGoblinLairCommand.java %BASE%\command\
move %BASE%\ReloadCustomModelsCommand.java %BASE%\command\
move %BASE%\SetModelCodeCommand.java %BASE%\command\
move %BASE%\WhitelistServerCommand.java %BASE%\command\

REM data
move %BASE%\CustomModelSavedData.java %BASE%\data\
move %BASE%\GalathOwnershipData.java %BASE%\data\
move %BASE%\CustomGirlNamesSavedData.java %BASE%\data\
move %BASE%\GalathSpawnListData.java %BASE%\data\

REM entity
move %BASE%\AllieEntity.java %BASE%\entity\
move %BASE%\BeeEntity.java %BASE%\entity\
move %BASE%\BiaEntity.java %BASE%\entity\
move %BASE%\ClothingOverlayEntity.java %BASE%\entity\
move %BASE%\EllieEntity.java %BASE%\entity\
move %BASE%\EnergyBallEntity.java %BASE%\entity\
move %BASE%\FishingHookEntity.java %BASE%\entity\
move %BASE%\GalathCombatDamageSource.java %BASE%\entity\
move %BASE%\GalathDamageSource.java %BASE%\entity\
move %BASE%\GalathEntity.java %BASE%\entity\
move %BASE%\GirlSpecificEntity.java %BASE%\entity\
move %BASE%\GoblinEntity.java %BASE%\entity\
move %BASE%\JennyEntity.java %BASE%\entity\
move %BASE%\KoboldColorVariant.java %BASE%\entity\
move %BASE%\KoboldEgg.java %BASE%\entity\
move %BASE%\KoboldEggEntity.java %BASE%\entity\
move %BASE%\KoboldEntity.java %BASE%\entity\
move %BASE%\KoboldName.java %BASE%\entity\
move %BASE%\KoboldNameList.java %BASE%\entity\
move %BASE%\LightingMode.java %BASE%\entity\
move %BASE%\LunaEntity.java %BASE%\entity\
move %BASE%\MangleLieEntity.java %BASE%\entity\
move %BASE%\NpcQueryInterface.java %BASE%\entity\
move %BASE%\NpcStateAccessor.java %BASE%\entity\
move %BASE%\NpcEnderPearl.java %BASE%\entity\
move %BASE%\NpcOpenDoorGoal.java %BASE%\entity\
move %BASE%\Resettable.java %BASE%\entity\
move %BASE%\SelectableEntityPart.java %BASE%\entity\
move %BASE%\SlimeEntity.java %BASE%\entity\
move %BASE%\TickableCallback.java %BASE%\entity\
move %BASE%\WanderingEnemyEntity.java %BASE%\entity\

REM entity.ai
move %BASE%\MangleLieAvoidGoal.java %BASE%\entity\ai\
move %BASE%\NpcBreedGoal.java %BASE%\entity\ai\
move %BASE%\NpcCombatGoal.java %BASE%\entity\ai\
move %BASE%\NpcGoalBase.java %BASE%\entity\ai\
move %BASE%\TribeAttackGoal.java %BASE%\entity\ai\
move %BASE%\KoboldFollowLeaderGoal.java %BASE%\entity\ai\
move %BASE%\ToggleableWatchGoal.java %BASE%\entity\ai\
move %BASE%\ToggleableWatchClosestGoal.java %BASE%\entity\ai\

REM event
move %BASE%\ArmorDamageHandler.java %BASE%\event\
move %BASE%\DevToolsHandler.java %BASE%\event\
move %BASE%\NpcDamageHandler.java %BASE%\event\
move %BASE%\NpcDeathHandler.java %BASE%\event\
move %BASE%\PlayerConnectionHandler.java %BASE%\event\
move %BASE%\PlayerSexEventHandler.java %BASE%\event\

REM handler
move %BASE%\BlockEventHandler.java %BASE%\handler\
move %BASE%\NameTagEventHandler.java %BASE%\handler\

REM inventory
move %BASE%\KoboldChestContainer.java %BASE%\inventory\
move %BASE%\NpcEquipmentContainer.java %BASE%\inventory\
move %BASE%\NpcInventoryContainer.java %BASE%\inventory\

REM item
move %BASE%\AlliesLampItem.java %BASE%\item\
move %BASE%\GalathCoinItem.java %BASE%\item\
move %BASE%\KoboldEggSpawnItem.java %BASE%\item\
move %BASE%\LunaRodItem.java %BASE%\item\
move %BASE%\NpcEditorWandItem.java %BASE%\item\
move %BASE%\StaffItem.java %BASE%\item\
move %BASE%\TribeEggItem.java %BASE%\item\
move %BASE%\WinchesterItem.java %BASE%\item\

REM network
move %BASE%\CatEatingDonePacket.java %BASE%\network\
move %BASE%\FakeClientNetHandler.java %BASE%\network\
move %BASE%\FakeNetworkManager.java %BASE%\network\
move %BASE%\ModNetwork.java %BASE%\network\
move %BASE%\OwnershipSyncPacket.java %BASE%\network\
move %BASE%\SendChatMessagePacket.java %BASE%\network\
move %BASE%\SendCompanionHomePacket.java %BASE%\network\
move %BASE%\CameraControlPacket.java %BASE%\network\
move %BASE%\ForcePlayerGirlUpdatePacket.java %BASE%\network\

REM network.packet
move %BASE%\BeeOpenChestPacket.java %BASE%\network\packet\
move %BASE%\CancelTaskPacket.java %BASE%\network\packet\
move %BASE%\CatActivateFishingPacket.java %BASE%\network\packet\
move %BASE%\CatThrowAwayItemPacket.java %BASE%\network\packet\
move %BASE%\ChangeDataParameterPacket.java %BASE%\network\packet\
move %BASE%\ClaimTribePacket.java %BASE%\network\packet\
move %BASE%\DespawnClothingPacket.java %BASE%\network\packet\
move %BASE%\FallTreePacket.java %BASE%\network\packet\
move %BASE%\GalathBackOffPacket.java %BASE%\network\packet\
move %BASE%\GalathRapePouncePacket.java %BASE%\network\packet\
move %BASE%\MakeRichWishPacket.java %BASE%\network\packet\
move %BASE%\MineAreaPacket.java %BASE%\network\packet\
move %BASE%\MineBlocksPacket.java %BASE%\network\packet\
move %BASE%\ModelListPacket.java %BASE%\network\packet\
move %BASE%\NpcSubtypeRenderer.java %BASE%\network\packet\
move %BASE%\OpenModelSelectPacket.java %BASE%\network\packet\
move %BASE%\OpenNpcInventoryPacket.java %BASE%\network\packet\
move %BASE%\RemoveItemsPacket.java %BASE%\network\packet\
move %BASE%\RequestRidingPacket.java %BASE%\network\packet\
move %BASE%\RequestServerModelAvailabilityPacket.java %BASE%\network\packet\
move %BASE%\ResetControllerPacket.java %BASE%\network\packet\
move %BASE%\ResetNpcPacket.java %BASE%\network\packet\
move %BASE%\SendEggPacket.java %BASE%\network\packet\
move %BASE%\SetNpcHomePacket.java %BASE%\network\packet\
move %BASE%\SetPlayerCamPacket.java %BASE%\network\packet\
move %BASE%\SetPlayerForNpcPacket.java %BASE%\network\packet\
move %BASE%\SetTribeFollowModePacket.java %BASE%\network\packet\
move %BASE%\SexPromptPacket.java %BASE%\network\packet\
move %BASE%\SpawnEnergyBallParticlesPacket.java %BASE%\network\packet\
move %BASE%\SpawnParticlePacket.java %BASE%\network\packet\
move %BASE%\StartGalathSexPacket.java %BASE%\network\packet\
move %BASE%\StartSexAnimationPacket.java %BASE%\network\packet\
move %BASE%\SummonAlliePacket.java %BASE%\network\packet\
move %BASE%\SyncCustomModelsPacket.java %BASE%\network\packet\
move %BASE%\SyncInventoryPacket.java %BASE%\network\packet\
move %BASE%\TeleportPlayerPacket.java %BASE%\network\packet\
move %BASE%\TransferOwnershipPacket.java %BASE%\network\packet\
move %BASE%\TribeHighlightPacket.java %BASE%\network\packet\
move %BASE%\TribeUIValuesPacket.java %BASE%\network\packet\
move %BASE%\UpdateEquipmentPacket.java %BASE%\network\packet\
move %BASE%\UpdateGalathVelocityPacket.java %BASE%\network\packet\
move %BASE%\UpdatePlayerModelPacket.java %BASE%\network\packet\

REM potion
move %BASE%\HornyPotion.java %BASE%\potion\

REM registry
move %BASE%\AnimState.java %BASE%\registry\
move %BASE%\ModItems.java %BASE%\registry\
move %BASE%\ModLootTables.java %BASE%\registry\
move %BASE%\ModSounds.java %BASE%\registry\

REM tribe
move %BASE%\TribeManager.java %BASE%\tribe\
move %BASE%\TribePhase.java %BASE%\tribe\
move %BASE%\TribeTask.java %BASE%\tribe\

REM util
move %BASE%\AngleTarget.java %BASE%\util\
move %BASE%\AngleUtil.java %BASE%\util\
move %BASE%\BiMap.java %BASE%\util\
move %BASE%\BoneMatrixUtil.java %BASE%\util\
move %BASE%\DirectionKey.java %BASE%\util\
move %BASE%\LightUtil.java %BASE%\util\
move %BASE%\MathUtil.java %BASE%\util\
move %BASE%\ModUtil.java %BASE%\util\
move %BASE%\NpcWorldUtil.java %BASE%\util\
move %BASE%\PathUtil.java %BASE%\util\
move %BASE%\PhysicsBoneUtil.java %BASE%\util\
move %BASE%\PhysicsParticle.java %BASE%\util\
move %BASE%\PlayerSkinUtil.java %BASE%\util\
move %BASE%\Rect2D.java %BASE%\util\
move %BASE%\RgbColor.java %BASE%\util\
move %BASE%\RgbaColor.java %BASE%\util\
move %BASE%\RgbaColorInner.java %BASE%\util\
move %BASE%\ThreadUtil.java %BASE%\util\
move %BASE%\VectorMathUtil.java %BASE%\util\
move %BASE%\Vec2D.java %BASE%\util\
move %BASE%\Vec2i.java %BASE%\util\
move %BASE%\YawPitch.java %BASE%\util\

REM world
move %BASE%\SexmodStructure.java %BASE%\world\
move %BASE%\SexmodStructureConstants.java %BASE%\world\
move %BASE%\SexmodFireBlock.java %BASE%\world\
move %BASE%\StructurePlacer.java %BASE%\world\
move %BASE%\WorldGenerationManager.java %BASE%\world\

echo Done! Files organized.
