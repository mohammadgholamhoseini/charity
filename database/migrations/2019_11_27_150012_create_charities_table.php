<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateCharitiesTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::create('charities', function (Blueprint $table) {
            $table->bigIncrements('id');
            $table->string('name');
            $table->string('owner');
            $table->integer('members');
            $table->text('description');
            $table->double('amountRaised')->nullable();
            $table->string('email')->unique();
            $table->string('address');
            $table->string('phone');
            $table->string('slug');
            $table->text('meta_desc');
            $table->text('meta_key');
            $table->text('meta_title');

            $table->unsignedBigInteger('category_id');
            $table->foreign('category_id')->references('id')->on('categories')->onDelete('cascade');

            $table->unsignedBigInteger('user_id');
            $table->foreign('user_id')->references('id')->on('users')->onDelete('cascade');

            $table->unsignedBigInteger('photo_id');
            $table->foreign('photo_id')->references('id')->on('photos')->onDelete('cascade');
            $table->timestamps();
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::dropIfExists('charities');
    }
}
