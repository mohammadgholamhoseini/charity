<?php

/*
|--------------------------------------------------------------------------
| Web Routes
|--------------------------------------------------------------------------
|
| Here is where you can register web routes for your application. These
| routes are loaded by the RouteServiceProvider within a group which
| contains the "web" middleware group. Now create something great!
|
*/

Route::get('/', function () {
    return view('welcome');
});
Route::get('/home', 'HomeController@index')->name('home');
Auth::routes();

Route::middleware(['auth'])->prefix('admin')->group(function () {
    Route::get('/', 'Admin\HomeController@index');
    Route::resource('/categories','Admin\CategoryController');
    Route::resource('/charities','Admin\CharityController');
    Route::resource('/photos','Admin\PhotoController');
});
